package wcg.yhm.note.service;

import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.ai.api.ModerationResult;
import wcg.yhm.note.api.domain.event.NoteRagIndexMessage;
import wcg.yhm.note.constants.MQConstant;
import wcg.yhm.note.constants.RedisKeyConstant;
import wcg.yhm.note.utils.CaffeineUtil;
import org.springframework.data.redis.core.RedisTemplate;
import wcg.yhm.note.model.dto.InsertNoteCountRecordDTO;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.Objects;

/** 根据图片和文本审核结果更新笔记状态，并防止旧版本结果覆盖编辑或人工审核。 */
@Service
public class NoteAiReviewDecisionService {
    @Resource private JdbcTemplate jdbc;
    @Resource private NoteRagOutboxService ragOutbox;
    @Resource private RocketMQTemplate rocketMQTemplate;
    @Resource private RedisTemplate<String, Object> redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void finish(Long taskId, Long noteId, int version, ModerationResult result) {
        // 图片必须明确通过；文本结果不合法时保留待审状态，转人工复核。
        boolean imagePass = result.isImagePass();
        String verdict = result.getTextVerdict();
        if (!Objects.equals(verdict, "PASS") && !Objects.equals(verdict, "REJECT")) verdict = "REVIEW";
        String textReason = result.getTextReason();
        if (Objects.equals(verdict, "REJECT") && (textReason == null || textReason.isBlank() || textReason.length() > 200)) {
            verdict = "REVIEW";
            textReason = "";
        }
        String error = result.getInternalError();
        if (error != null && error.length() > 1000) error = error.substring(0, 1000);
        int recorded = jdbc.update("UPDATE note_ai_review_task SET text_result=?, text_reason=?, image_pass=?, internal_error=? WHERE id=? AND status='RUNNING'",
                verdict, textReason, imagePass, error, taskId);
        if (recorded == 0) return; // 其他工作线程或管理员已经处理了此任务。

        Outcome outcome = classify(result);
        if (outcome.approved() == null) {
            jdbc.update("UPDATE note_ai_review_task SET status='HUMAN_REQUIRED', lease_until=NULL WHERE id=? AND status='RUNNING'", taskId);
            return;
        }
        boolean pass = outcome.approved();
        String reason = outcome.reason();
        // 仅允许当前内容版本、仍在待审且尚无人审核的笔记落地 AI 结论。
        int changed = jdbc.update("UPDATE note SET status=?, review_method='AI', review_reason=?, reviewed_at=NOW(), update_time=NOW() "
                + "WHERE id=? AND status=0 AND type=0 AND review_version=? AND review_method IS NULL",
                pass ? 1 : 3, reason, noteId, version);
        if (changed == 0) {
            jdbc.update("UPDATE note_ai_review_task SET status='STALE', lease_until=NULL WHERE id=? AND status='RUNNING'", taskId);
            return;
        }
        jdbc.update("UPDATE note_ai_review_task SET status=?, lease_until=NULL WHERE id=? AND status='RUNNING'",
                pass ? "PASSED" : "REJECTED", taskId);
        ragOutbox.append(noteId, pass ? NoteRagIndexMessage.EVENT_UPSERT : NoteRagIndexMessage.EVENT_DELETE,
                LocalDateTime.now());
        redisTemplate.delete(RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId);
        CaffeineUtil.deleteNoteDetail(noteId);
        rocketMQTemplate.asyncSend(MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId, new SendCallback() {
            public void onSuccess(SendResult sendResult) { }
            public void onException(Throwable throwable) { }
        });
        if (pass) {
            rocketMQTemplate.asyncSend(MQConstant.TOPIC_INSERT_NOTE_COUNT_RECORD,
                    MessageBuilder.withPayload(JsonUtil.toJsonString(new InsertNoteCountRecordDTO(noteId))).build(),
                    new SendCallback() {
                        public void onSuccess(SendResult sendResult) { }
                        public void onException(Throwable throwable) { }
                    });
        }
    }

    public static Outcome classify(ModerationResult result) {
        // 图片驳回统一使用作者可见文案；模型异常的细节只保留在任务内部记录。
        if (result == null || !result.isImagePass()) return new Outcome(false, "图片不合规");
        if ("PASS".equals(result.getTextVerdict()) && "".equals(result.getTextReason())) {
            return new Outcome(true, null);
        }
        String reason = result.getTextReason();
        if ("REJECT".equals(result.getTextVerdict()) && reason != null
                && !reason.isBlank() && reason.length() <= 200) {
            return new Outcome(false, reason.trim());
        }
        return new Outcome(null, null);
    }

    public record Outcome(Boolean approved, String reason) { }
}
