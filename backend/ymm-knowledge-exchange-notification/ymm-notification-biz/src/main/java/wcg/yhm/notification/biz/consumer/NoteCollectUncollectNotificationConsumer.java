package wcg.yhm.notification.biz.consumer;

import cn.hutool.crypto.digest.DigestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.note.api.domain.event.MQCollectUnCollectDTO;
import wcg.yhm.notification.biz.domain.bo.NotificationMutationResult;
import wcg.yhm.notification.biz.service.NotificationEventService;
import wcg.yhm.notification.biz.service.NotificationFeedCacheService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "ymm_group_notification_note_collect_uncollect",
        topic = "NoteCollectOrUnCollect",
        consumeMode = ConsumeMode.ORDERLY)
public class NoteCollectUncollectNotificationConsumer implements RocketMQListener<Message> {
    private static final String TAG_COLLECT = "Collect";
    private static final String TAG_UNCOLLECT = "UnCollect";

    @Resource
    private NotificationEventService eventService;
    @Resource
    private NotificationFeedCacheService feedCacheService;

    @Override
    public void onMessage(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String tag = message.getTags();
        MQCollectUnCollectDTO event = JsonUtil.parseObject(body, MQCollectUnCollectDTO.class);
        if (!valid(event)) {
            log.warn("忽略无效或自己收藏自己的通知消息, tag={}, body={}", tag, body);
            return;
        }

        String eventId = eventId(event.getOperationId(), message, tag, body);
        NotificationMutationResult result;
        if (TAG_COLLECT.equals(tag)) {
            result = eventService.noteCollect(eventId, event.getUserId(), event.getCreatorId(),
                    event.getNoteId(), event.getCreateTime());
        } else if (TAG_UNCOLLECT.equals(tag)) {
            result = eventService.noteUncollect(eventId, event.getUserId(), event.getCreatorId(),
                    event.getNoteId(), event.getCreateTime());
        } else {
            log.warn("忽略未知收藏通知标签: {}", tag);
            return;
        }

        try {
            feedCacheService.applyMutation(result);
        } catch (Exception e) {
            log.warn("收藏通知已写入MySQL，但Redis Feed更新失败，将由查询流程重建, userId={}",
                    result.getRecipientId(), e);
            try {
                feedCacheService.evictFeed(result.getRecipientId());
            } catch (Exception ignored) {
                // Redis 不可用时不阻塞通知事实写入。
            }
        }
    }

    private boolean valid(MQCollectUnCollectDTO event) {
        return event != null
                && event.getUserId() != null
                && event.getCreatorId() != null
                && event.getNoteId() != null
                && !Objects.equals(event.getUserId(), event.getCreatorId());
    }

    private String eventId(String operationId, Message message, String tag, String body) {
        if (operationId != null && !operationId.isBlank()) {
            return operationId;
        }
        if (message instanceof MessageExt messageExt
                && messageExt.getMsgId() != null && !messageExt.getMsgId().isBlank()) {
            return messageExt.getMsgId();
        }
        return DigestUtil.sha256Hex(tag + ":" + body);
    }
}
