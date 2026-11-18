package wcg.yhm.note.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wcg.yhm.note.api.domain.event.NoteRagIndexMessage;
import wcg.yhm.note.domain.dataobject.NoteRagOutbox;
import wcg.yhm.note.domain.mapper.NoteRagOutboxMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.List;

/** 可重试、允许重复投递的事务 Outbox 发布器。 */
@Slf4j
@Component
public class NoteRagOutboxPublisher {

    @Resource
    private NoteRagOutboxMapper mapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rag.note.outbox.batch-size:50}")
    private int batchSize;
    @Value("${rag.note.outbox.max-retry-seconds:300}")
    private int maxRetrySeconds;

    @Scheduled(fixedDelayString = "${rag.note.outbox.publish-delay-ms:1000}")
    public void publishPending() {
        List<NoteRagOutbox> events = mapper.selectList(Wrappers.<NoteRagOutbox>lambdaQuery()
                .in(NoteRagOutbox::getStatus, 0, 2)
                .le(NoteRagOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(NoteRagOutbox::getId)
                .last("LIMIT " + Math.max(1, batchSize)));
        events.forEach(this::publishOne);
    }

    private void publishOne(NoteRagOutbox event) {
        try {
            NoteRagIndexMessage message = NoteRagIndexMessage.builder()
                    .eventId(event.getEventId()).noteId(event.getNoteId()).eventType(event.getEventType())
                    .noteUpdatedAt(event.getNoteUpdatedAt()).schemaVersion(event.getSchemaVersion())
                    .triggerTime(LocalDateTime.now()).build();
            String destination = NoteRagIndexMessage.TOPIC_NOTE_RAG_INDEX + ":" + NoteRagIndexMessage.TAG_SYNC;
            // MQ 确认后才标记已发送；若确认后进程退出，重复投递由消费者去重。
            rocketMQTemplate.syncSend(destination, JsonUtil.toJsonString(message));
            mapper.update(null, Wrappers.<NoteRagOutbox>lambdaUpdate()
                    .eq(NoteRagOutbox::getId, event.getId()).ne(NoteRagOutbox::getStatus, 1)
                    .set(NoteRagOutbox::getStatus, 1).set(NoteRagOutbox::getErrorMessage, null)
                    .set(NoteRagOutbox::getUpdateTime, LocalDateTime.now()));
        } catch (Exception e) {
            int attempts = (event.getAttempts() == null ? 0 : event.getAttempts()) + 1;
            long delay = Math.min(Math.max(1, maxRetrySeconds), 1L << Math.min(attempts, 8));
            mapper.update(null, Wrappers.<NoteRagOutbox>lambdaUpdate()
                    .eq(NoteRagOutbox::getId, event.getId()).ne(NoteRagOutbox::getStatus, 1)
                    .set(NoteRagOutbox::getStatus, 2).set(NoteRagOutbox::getAttempts, attempts)
                    .set(NoteRagOutbox::getNextRetryTime, LocalDateTime.now().plusSeconds(delay))
                    .set(NoteRagOutbox::getErrorMessage,
                            StrUtil.sub(StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()), 0, 512))
                    .set(NoteRagOutbox::getUpdateTime, LocalDateTime.now()));
            log.warn("RAG Outbox投递失败，将重试: eventId={}, attempts={}", event.getEventId(), attempts, e);
        }
    }
}
