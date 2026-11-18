package wcg.yhm.note.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wcg.yhm.note.api.domain.event.NoteModerationEvent;
import wcg.yhm.note.domain.dataobject.NoteNotificationOutbox;
import wcg.yhm.note.domain.mapper.NoteNotificationOutboxMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.List;

/** 定时投递笔记审核事件；失败时退避重试，消费端需按事件 ID 去重。 */
@Slf4j
@Component
public class NoteNotificationOutboxPublisher {
    @Resource
    private NoteNotificationOutboxMapper mapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${notification.note.outbox.batch-size:50}")
    private int batchSize;
    @Value("${notification.note.outbox.max-retry-seconds:300}")
    private int maxRetrySeconds;

    @Scheduled(fixedDelayString = "${notification.note.outbox.publish-delay-ms:1000}")
    public void publishPending() {
        List<NoteNotificationOutbox> events = mapper.selectList(
                Wrappers.<NoteNotificationOutbox>lambdaQuery()
                        .in(NoteNotificationOutbox::getStatus, 0, 2)
                        .le(NoteNotificationOutbox::getNextRetryTime, LocalDateTime.now())
                        .orderByAsc(NoteNotificationOutbox::getId)
                        .last("LIMIT " + Math.max(1, batchSize)));
        events.forEach(this::publishOne);
    }

    private void publishOne(NoteNotificationOutbox event) {
        try {
            NoteModerationEvent message = NoteModerationEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .noteId(event.getNoteId())
                    .recipientId(event.getRecipientId())
                    .noteTitle(event.getNoteTitle())
                    .reasonCode(event.getReasonCode())
                    .reason(event.getReason())
                    .occurredAt(event.getOccurredAt())
                    .schemaVersion(event.getSchemaVersion())
                    .build();
            String destination = NoteModerationEvent.TOPIC + ":" + NoteModerationEvent.TAG_DOWNED;
            // 消息确认发送后才标记成功；崩溃时可能重复投递，由消费端保证幂等。
            rocketMQTemplate.syncSend(destination, JsonUtil.toJsonString(message));
            mapper.update(null, Wrappers.<NoteNotificationOutbox>lambdaUpdate()
                    .eq(NoteNotificationOutbox::getId, event.getId())
                    .ne(NoteNotificationOutbox::getStatus, 1)
                    .set(NoteNotificationOutbox::getStatus, 1)
                    .set(NoteNotificationOutbox::getErrorMessage, null)
                    .set(NoteNotificationOutbox::getUpdateTime, LocalDateTime.now()));
        } catch (Exception e) {
            int attempts = (event.getAttempts() == null ? 0 : event.getAttempts()) + 1;
            long delay = Math.min(Math.max(1, maxRetrySeconds), 1L << Math.min(attempts, 8));
            mapper.update(null, Wrappers.<NoteNotificationOutbox>lambdaUpdate()
                    .eq(NoteNotificationOutbox::getId, event.getId())
                    .ne(NoteNotificationOutbox::getStatus, 1)
                    .set(NoteNotificationOutbox::getStatus, 2)
                    .set(NoteNotificationOutbox::getAttempts, attempts)
                    .set(NoteNotificationOutbox::getNextRetryTime, LocalDateTime.now().plusSeconds(delay))
                    .set(NoteNotificationOutbox::getErrorMessage,
                            StrUtil.sub(StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()), 0, 512))
                    .set(NoteNotificationOutbox::getUpdateTime, LocalDateTime.now()));
            log.warn("笔记下架通知Outbox投递失败，将重试: eventId={}, attempts={}",
                    event.getEventId(), attempts, e);
        }
    }
}
