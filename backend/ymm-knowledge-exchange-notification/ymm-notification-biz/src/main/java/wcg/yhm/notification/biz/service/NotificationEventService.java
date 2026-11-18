package wcg.yhm.notification.biz.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.notification.biz.constants.NotificationConstants;
import wcg.yhm.notification.biz.domain.bo.NotificationMutationResult;
import wcg.yhm.notification.biz.domain.po.Notification;
import wcg.yhm.notification.biz.domain.po.NotificationEventLog;
import wcg.yhm.notification.biz.mapper.NotificationEventLogMapper;
import wcg.yhm.notification.biz.mapper.NotificationMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/** 消费关注和互动事件，按事件 ID 去重，并维护当前有效的通知记录。 */
@Service
public class NotificationEventService {
    @Resource
    private NotificationMapper notificationMapper;
    @Resource
    private NotificationEventLogMapper eventLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public NotificationMutationResult follow(String eventId, Long actorId, Long recipientId,
                                             LocalDateTime occurredAt) {
        if (!recordEvent(eventId, NotificationConstants.EVENT_FOLLOW)) {
            return duplicate(recipientId);
        }

        LocalDateTime now = occurredAt == null ? LocalDateTime.now() : occurredAt;
        List<Long> removedIds = activeNotificationIds(
                actorId, recipientId, NotificationConstants.TYPE_FOLLOW, null);
        revoke(actorId, recipientId, NotificationConstants.TYPE_FOLLOW, null, now);

        Notification notification = new Notification()
                .setEventId(eventId)
                .setRecipientId(recipientId)
                .setActorId(actorId)
                .setType(NotificationConstants.TYPE_FOLLOW)
                .setStatus(NotificationConstants.STATUS_ACTIVE)
                .setIsRead(false)
                .setCreateTime(now)
                .setUpdateTime(now);
        notificationMapper.insert(notification);

        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .addedNotificationId(notification.getId())
                .removedNotificationIds(removedIds)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationMutationResult unfollow(String eventId, Long actorId, Long recipientId,
                                               LocalDateTime occurredAt) {
        if (!recordEvent(eventId, NotificationConstants.EVENT_UNFOLLOW)) {
            return duplicate(recipientId);
        }
        List<Long> removedIds = activeNotificationIds(
                actorId, recipientId, NotificationConstants.TYPE_FOLLOW, null);
        revoke(actorId, recipientId, NotificationConstants.TYPE_FOLLOW, null,
                occurredAt == null ? LocalDateTime.now() : occurredAt);
        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .removedNotificationIds(removedIds)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationMutationResult noteLike(String eventId, Long actorId, Long recipientId,
                                               Long noteId, LocalDateTime occurredAt) {
        if (!recordEvent(eventId, NotificationConstants.EVENT_NOTE_LIKE)) {
            return duplicate(recipientId);
        }

        LocalDateTime now = occurredAt == null ? LocalDateTime.now() : occurredAt;
        List<Long> removedIds = activeNotificationIds(
                actorId, recipientId, NotificationConstants.TYPE_NOTE_LIKE, noteId);
        revoke(actorId, recipientId, NotificationConstants.TYPE_NOTE_LIKE, noteId, now);

        Notification notification = new Notification()
                .setEventId(eventId)
                .setRecipientId(recipientId)
                .setActorId(actorId)
                .setType(NotificationConstants.TYPE_NOTE_LIKE)
                .setTargetId(noteId)
                .setStatus(NotificationConstants.STATUS_ACTIVE)
                .setIsRead(false)
                .setCreateTime(now)
                .setUpdateTime(now);
        notificationMapper.insert(notification);

        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .addedNotificationId(notification.getId())
                .removedNotificationIds(removedIds)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationMutationResult noteUnlike(String eventId, Long actorId, Long recipientId,
                                                 Long noteId, LocalDateTime occurredAt) {
        if (!recordEvent(eventId, NotificationConstants.EVENT_NOTE_UNLIKE)) {
            return duplicate(recipientId);
        }
        List<Long> removedIds = activeNotificationIds(
                actorId, recipientId, NotificationConstants.TYPE_NOTE_LIKE, noteId);
        revoke(actorId, recipientId, NotificationConstants.TYPE_NOTE_LIKE, noteId,
                occurredAt == null ? LocalDateTime.now() : occurredAt);
        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .removedNotificationIds(removedIds)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationMutationResult noteCollect(String eventId, Long actorId, Long recipientId,
                                                  Long noteId, LocalDateTime occurredAt) {
        if (!recordEvent(eventId, NotificationConstants.EVENT_NOTE_COLLECT)) {
            return duplicate(recipientId);
        }

        LocalDateTime now = occurredAt == null ? LocalDateTime.now() : occurredAt;
        List<Long> removedIds = activeNotificationIds(
                actorId, recipientId, NotificationConstants.TYPE_NOTE_COLLECT, noteId);
        revoke(actorId, recipientId, NotificationConstants.TYPE_NOTE_COLLECT, noteId, now);

        Notification notification = new Notification()
                .setEventId(eventId)
                .setRecipientId(recipientId)
                .setActorId(actorId)
                .setType(NotificationConstants.TYPE_NOTE_COLLECT)
                .setTargetId(noteId)
                .setStatus(NotificationConstants.STATUS_ACTIVE)
                .setIsRead(false)
                .setCreateTime(now)
                .setUpdateTime(now);
        notificationMapper.insert(notification);

        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .addedNotificationId(notification.getId())
                .removedNotificationIds(removedIds)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationMutationResult noteUncollect(String eventId, Long actorId, Long recipientId,
                                                    Long noteId, LocalDateTime occurredAt) {
        if (!recordEvent(eventId, NotificationConstants.EVENT_NOTE_UNCOLLECT)) {
            return duplicate(recipientId);
        }
        List<Long> removedIds = activeNotificationIds(
                actorId, recipientId, NotificationConstants.TYPE_NOTE_COLLECT, noteId);
        revoke(actorId, recipientId, NotificationConstants.TYPE_NOTE_COLLECT, noteId,
                occurredAt == null ? LocalDateTime.now() : occurredAt);
        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .removedNotificationIds(removedIds)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationMutationResult noteDowned(String eventId, Long recipientId, Long noteId,
                                                  String noteTitle, String reasonCode, String reason,
                                                  LocalDateTime occurredAt) {
        if (!recordEvent(eventId, NotificationConstants.EVENT_NOTE_DOWNED)) {
            return duplicate(recipientId);
        }

        LocalDateTime now = occurredAt == null ? LocalDateTime.now() : occurredAt;
        Notification notification = new Notification()
                .setEventId(eventId)
                .setRecipientId(recipientId)
                .setActorId(null)
                .setType(NotificationConstants.TYPE_NOTE_DOWNED)
                .setTargetId(noteId)
                .setTargetTitle(noteTitle)
                .setReasonCode(reasonCode)
                .setReason(reason)
                .setStatus(NotificationConstants.STATUS_ACTIVE)
                .setIsRead(false)
                .setCreateTime(now)
                .setUpdateTime(now);
        notificationMapper.insert(notification);

        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .addedNotificationId(notification.getId())
                .removedNotificationIds(Collections.emptyList())
                .build();
    }

    private boolean recordEvent(String eventId, String eventType) {
        // 事件日志与通知变更处于同一事务，防止 MQ 重复消费制造重复通知。
        NotificationEventLog eventLog = new NotificationEventLog()
                .setEventId(eventId)
                .setEventType(eventType)
                .setCreateTime(LocalDateTime.now());
        return eventLogMapper.insertIgnore(eventLog) > 0;
    }

    private List<Long> activeNotificationIds(Long actorId, Long recipientId, Integer type, Long targetId) {
        var query = Wrappers.<Notification>lambdaQuery()
                .select(Notification::getId)
                .eq(Notification::getActorId, actorId)
                .eq(Notification::getRecipientId, recipientId)
                .eq(Notification::getType, type)
                .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE);
        if (targetId == null) {
            query.isNull(Notification::getTargetId);
        } else {
            query.eq(Notification::getTargetId, targetId);
        }
        return notificationMapper.selectList(query)
                .stream().map(Notification::getId).toList();
    }

    private void revoke(Long actorId, Long recipientId, Integer type, Long targetId, LocalDateTime now) {
        var update = Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getActorId, actorId)
                .eq(Notification::getRecipientId, recipientId)
                .eq(Notification::getType, type)
                .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE);
        if (targetId == null) {
            update.isNull(Notification::getTargetId);
        } else {
            update.eq(Notification::getTargetId, targetId);
        }
        notificationMapper.update(null, update
                .set(Notification::getStatus, NotificationConstants.STATUS_REVOKED)
                .set(Notification::getRevokeTime, now)
                .set(Notification::getUpdateTime, now));
    }

    private NotificationMutationResult duplicate(Long recipientId) {
        return NotificationMutationResult.builder()
                .recipientId(recipientId)
                .removedNotificationIds(Collections.emptyList())
                .duplicate(true)
                .build();
    }
}
