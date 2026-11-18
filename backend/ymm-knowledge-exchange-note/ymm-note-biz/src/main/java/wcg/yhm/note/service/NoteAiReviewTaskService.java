package wcg.yhm.note.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.ai.api.ModerationFeignApi;
import wcg.yhm.ai.api.ModerationRequest;
import wcg.yhm.ai.api.ModerationResult;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.domain.mapper.NoteMapper;
import wcg.yhm.note.rpc.TextRpcService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map;

/** 持久化 AI 审核任务：轮询领取任务并交给线程池执行，租约超时后允许重新领取。 */
@Service
@Slf4j
public class NoteAiReviewTaskService {
    @Resource private JdbcTemplate jdbc;
    @Resource private NoteMapper noteMapper;
    @Resource private TextRpcService textRpcService;
    @Resource private ModerationFeignApi moderationFeignApi;
    @Resource private NoteAiReviewDecisionService decisionService;
    @Resource(name = "noteReviewExecutor") private ThreadPoolTaskExecutor reviewExecutor;

    /** 与笔记新增或编辑处于同一数据库事务，保证待审笔记与审核任务一起提交。 */
    public void enqueue(Long noteId, int version) {
        jdbc.update("INSERT INTO note_ai_review_task(note_id,review_version,status) VALUES (?,?,'QUEUED') "
                + "ON DUPLICATE KEY UPDATE id=id", noteId, version);
    }

    public void markOverridden(Long noteId, Integer version) {
        jdbc.update("UPDATE note_ai_review_task SET status='OVERRIDDEN', lease_until=NULL "
                + "WHERE note_id=? AND review_version=? AND status IN ('QUEUED','RUNNING','PASSED','REJECTED','HUMAN_REQUIRED')",
                noteId, version);
    }

    public Map<String, Object> latest(Long noteId, Integer version) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT status, text_result, image_pass, internal_error "
                + "FROM note_ai_review_task WHERE note_id=? AND review_version=? LIMIT 1", noteId, version);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    @Scheduled(fixedDelayString = "${note.ai-review.poll-ms:5000}")
    public void poll() {
        // 使用条件更新抢占任务，避免多个实例重复执行；线程池满时退回队列等待下次轮询。
        List<Long> ids = jdbc.queryForList("SELECT id FROM note_ai_review_task "
                + "WHERE status='QUEUED' OR (status='RUNNING' AND lease_until < NOW()) "
                + "ORDER BY id LIMIT 4", Long.class);
        for (Long id : ids) {
            if (jdbc.update("UPDATE note_ai_review_task SET status='RUNNING', lease_until=DATE_ADD(NOW(), INTERVAL 10 MINUTE) "
                    + "WHERE id=? AND (status='QUEUED' OR (status='RUNNING' AND lease_until < NOW()))", id) == 1) {
                try {
                    reviewExecutor.execute(() -> {
                        try { process(id); } catch (Exception e) { log.error("AI审核任务处理失败, taskId={}", id, e); }
                    });
                } catch (RejectedExecutionException e) {
                    jdbc.update("UPDATE note_ai_review_task SET status='QUEUED', lease_until=NULL WHERE id=? AND status='RUNNING'", id);
                }
            }
        }
    }

    private void process(Long taskId) {
        var tasks = jdbc.queryForList("SELECT note_id, review_version FROM note_ai_review_task WHERE id=?", taskId);
        if (tasks.isEmpty()) return;
        Long noteId = ((Number) tasks.get(0).get("note_id")).longValue();
        int version = ((Number) tasks.get(0).get("review_version")).intValue();
        Note note = noteMapper.selectById(noteId);
        if (!isCurrent(note, version)) {
            jdbc.update("UPDATE note_ai_review_task SET status='STALE', lease_until=NULL WHERE id=? AND status='RUNNING'", taskId);
            return;
        }
        ModerationResult result;
        try {
            ModerationRequest request = new ModerationRequest();
            request.setTitle(note.getTitle());
            String content = Boolean.TRUE.equals(note.getIsContentEmpty()) ? "" : textRpcService.getContent(note.getContentUuid());
            request.setTextUnavailable(content == null);
            request.setContent(content);
            request.setImageUrls(note.getImgUris() == null ? List.of() : Arrays.asList(note.getImgUris().split(",")));
            result = moderationFeignApi.review(request);
            if (result == null) throw new IllegalStateException("AI审核无结果");
        } catch (Exception e) {
            // 图片调用失败按不合规处理；文本无法判断则交给人工复核。
            result = new ModerationResult();
            result.setImagePass(false);
            result.setTextVerdict("REVIEW");
            result.setInternalError("模型调用失败: " + e.getClass().getSimpleName());
            log.warn("AI审核调用失败，图片按不合规处理, noteId={}", noteId, e);
        }
        decisionService.finish(taskId, noteId, version, result);
    }

    public static boolean isCurrent(Note note, int version) {
        return note != null && Objects.equals(note.getStatus(), 0)
                && Objects.equals(note.getType(), 0)
                && Objects.equals(note.getReviewVersion(), version);
    }
}
