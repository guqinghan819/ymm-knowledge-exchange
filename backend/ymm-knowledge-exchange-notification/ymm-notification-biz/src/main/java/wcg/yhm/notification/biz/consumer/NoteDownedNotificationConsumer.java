package wcg.yhm.notification.biz.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.note.api.domain.event.NoteModerationEvent;
import wcg.yhm.notification.biz.domain.bo.NotificationMutationResult;
import wcg.yhm.notification.biz.service.NotificationEventService;
import wcg.yhm.notification.biz.service.NotificationFeedCacheService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "ymm_group_notification_note_moderation",
        topic = NoteModerationEvent.TOPIC,
        selectorExpression = NoteModerationEvent.TAG_DOWNED,
        consumeMode = ConsumeMode.ORDERLY)
public class NoteDownedNotificationConsumer implements RocketMQListener<Message> {
    @Resource
    private NotificationEventService eventService;
    @Resource
    private NotificationFeedCacheService feedCacheService;

    @Override
    public void onMessage(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        NoteModerationEvent event = JsonUtil.parseObject(body, NoteModerationEvent.class);
        if (!valid(event)) {
            log.warn("忽略无效的笔记下架通知消息, body={}", body);
            return;
        }

        NotificationMutationResult result = eventService.noteDowned(
                event.getEventId(), event.getRecipientId(), event.getNoteId(), event.getNoteTitle(),
                event.getReasonCode(), event.getReason(), event.getOccurredAt());
        try {
            feedCacheService.applyMutation(result);
        } catch (Exception e) {
            log.warn("笔记下架通知已写入MySQL，但Redis Feed更新失败，将由查询流程重建, userId={}",
                    result.getRecipientId(), e);
            try {
                feedCacheService.evictFeed(result.getRecipientId());
            } catch (Exception ignored) {
                // Redis 不可用时不阻塞通知事实写入。
            }
        }
    }

    private boolean valid(NoteModerationEvent event) {
        return event != null
                && event.getEventId() != null && !event.getEventId().isBlank()
                && NoteModerationEvent.EVENT_NOTE_DOWNED.equals(event.getEventType())
                && event.getNoteId() != null
                && event.getRecipientId() != null
                && event.getReasonCode() != null && !event.getReasonCode().isBlank()
                && event.getReason() != null && !event.getReason().isBlank();
    }
}
