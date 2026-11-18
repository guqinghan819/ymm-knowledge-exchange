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
import wcg.yhm.notification.biz.constants.NotificationConstants;
import wcg.yhm.notification.biz.domain.bo.NotificationMutationResult;
import wcg.yhm.notification.biz.service.NotificationEventService;
import wcg.yhm.notification.biz.service.NotificationFeedCacheService;
import wcg.yhm.user.relation.api.domain.event.FollowUserMQRequest;
import wcg.yhm.user.relation.api.domain.event.UnFollowUserMQRequest;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = "ymm_group_notification_follow_unfollow",
        topic = "FollowUnfollowTopic",
        consumeMode = ConsumeMode.ORDERLY)
public class FollowUnfollowNotificationConsumer implements RocketMQListener<Message> {
    @Resource
    private NotificationEventService eventService;
    @Resource
    private NotificationFeedCacheService feedCacheService;

    @Override
    public void onMessage(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String tag = message.getTags();
        NotificationMutationResult result;
        if ("Follow".equals(tag)) {
            FollowUserMQRequest event = JsonUtil.parseObject(body, FollowUserMQRequest.class);
            if (!valid(event == null ? null : event.getUserId(), event == null ? null : event.getFollowUserId())) {
                return;
            }
            result = eventService.follow(eventId(event.getOperationId(), message, tag, body), event.getUserId(),
                    event.getFollowUserId(), event.getCreateTime());
        } else if ("Unfollow".equals(tag)) {
            UnFollowUserMQRequest event = JsonUtil.parseObject(body, UnFollowUserMQRequest.class);
            if (!valid(event == null ? null : event.getUserId(), event == null ? null : event.getUnfollowUserId())) {
                return;
            }
            result = eventService.unfollow(eventId(event.getOperationId(), message, tag, body), event.getUserId(),
                    event.getUnfollowUserId(), event.getCreateTime());
        } else {
            log.warn("忽略未知关注通知标签: {}", tag);
            return;
        }

        try {
            feedCacheService.applyMutation(result);
        } catch (Exception e) {
            log.warn("通知已写入MySQL，但Redis Feed更新失败，将由查询流程重建, userId={}", result.getRecipientId(), e);
            try {
                feedCacheService.evictFeed(result.getRecipientId());
            } catch (Exception ignored) {
                // Redis 不可用时不阻塞通知事实写入。
            }
        }
    }

    private boolean valid(Long actorId, Long recipientId) {
        return actorId != null && recipientId != null && !Objects.equals(actorId, recipientId);
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
