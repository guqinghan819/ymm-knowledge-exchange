package wcg.yhm.notification.biz.constants;

public interface RedisKeyConstants {
    String FEED_PREFIX = "notification:feed:";
    String FEED_READY_PREFIX = "notification:feed:ready:";
    String UNREAD_COUNT_PREFIX = "notification:unread:";

    static String feed(Long userId) {
        return FEED_PREFIX + userId;
    }

    static String feedReady(Long userId) {
        return FEED_READY_PREFIX + userId;
    }

    static String unreadCount(Long userId) {
        return UNREAD_COUNT_PREFIX + userId;
    }
}
