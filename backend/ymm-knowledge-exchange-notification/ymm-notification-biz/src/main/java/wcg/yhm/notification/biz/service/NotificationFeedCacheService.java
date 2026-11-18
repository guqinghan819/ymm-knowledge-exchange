package wcg.yhm.notification.biz.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import wcg.yhm.notification.biz.constants.RedisKeyConstants;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class NotificationFeedCacheService {
    private static final long MAX_FEED_SIZE = 500;
    private static final Duration FEED_TTL = Duration.ofDays(7);
    private static final Duration READY_TTL = FEED_TTL;
    private static final Duration UNREAD_TTL = Duration.ofMinutes(5);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public List<Long> firstPageIds(Long userId, int limit) {
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyConstants.feedReady(userId)))) {
                return null;
            }
            Set<Object> values = redisTemplate.opsForZSet()
                    .reverseRange(RedisKeyConstants.feed(userId), 0, limit);
            if (values == null) {
                return Collections.emptyList();
            }
            return values.stream().map(value -> Long.valueOf(String.valueOf(value))).toList();
        } catch (Exception e) {
            log.warn("读取通知Feed缓存失败, userId={}", userId, e);
            return null;
        }
    }

    public void rebuild(Long userId, List<Long> notificationIds) {
        String feedKey = RedisKeyConstants.feed(userId);
        String readyKey = RedisKeyConstants.feedReady(userId);
        redisTemplate.delete(feedKey);
        if (notificationIds != null && !notificationIds.isEmpty()) {
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples =
                    new java.util.HashSet<>();
            notificationIds.forEach(id -> tuples.add(
                    org.springframework.data.redis.core.ZSetOperations.TypedTuple.of(id.toString(), id.doubleValue())));
            redisTemplate.opsForZSet().add(feedKey, tuples);
            redisTemplate.expire(feedKey, FEED_TTL);
        }
        redisTemplate.opsForValue().set(readyKey, true, READY_TTL);
    }

    public void applyMutation(wcg.yhm.notification.biz.domain.bo.NotificationMutationResult result) {
        if (result == null || result.getRecipientId() == null) {
            return;
        }
        Long userId = result.getRecipientId();
        if (result.isDuplicate()) {
            evictFeed(userId);
            invalidateUnread(userId);
            return;
        }
        String feedKey = RedisKeyConstants.feed(userId);
        if (result.getRemovedNotificationIds() != null && !result.getRemovedNotificationIds().isEmpty()) {
            Object[] members = result.getRemovedNotificationIds().stream().map(String::valueOf).toArray();
            redisTemplate.opsForZSet().remove(feedKey, members);
        }
        if (result.getAddedNotificationId() != null
                && Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyConstants.feedReady(userId)))) {
            Long id = result.getAddedNotificationId();
            redisTemplate.opsForZSet().add(feedKey, id.toString(), id.doubleValue());
            Long size = redisTemplate.opsForZSet().zCard(feedKey);
            if (size != null && size > MAX_FEED_SIZE) {
                redisTemplate.opsForZSet().removeRange(feedKey, 0, size - MAX_FEED_SIZE - 1);
            }
            redisTemplate.expire(feedKey, FEED_TTL);
        }
        invalidateUnread(userId);
    }

    public Long getUnread(Long userId) {
        try {
            Object value = redisTemplate.opsForValue().get(RedisKeyConstants.unreadCount(userId));
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            log.warn("读取通知未读数缓存失败, userId={}", userId, e);
            return null;
        }
    }

    public void cacheUnread(Long userId, Long count) {
        try {
            redisTemplate.opsForValue().set(RedisKeyConstants.unreadCount(userId), count, UNREAD_TTL);
        } catch (Exception e) {
            log.warn("写入通知未读数缓存失败, userId={}", userId, e);
        }
    }

    public void invalidateUnread(Long userId) {
        try {
            redisTemplate.delete(RedisKeyConstants.unreadCount(userId));
        } catch (Exception e) {
            log.warn("清理通知未读数缓存失败, userId={}", userId, e);
        }
    }

    public void evictFeed(Long userId) {
        redisTemplate.delete(List.of(
                RedisKeyConstants.feed(userId),
                RedisKeyConstants.feedReady(userId)));
    }
}
