package wcg.yhm.comment.biz.constants;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: redis键
 * @Version: 1.0
 */

public class RedisKeyConstants {

    /**
     * 一级评论最早回复评论键
     */
    public static final String HAVE_COMMENT_EARLIEST_REPLY_PREFIX = "comment:earliestReply:";

    public static final String COMMENT_ONE_LEVEL_VERSION_PREFIX = "comment:cache:version:one:";

    public static final String COMMENT_REPLY_VERSION_PREFIX = "comment:cache:version:reply:";

    public static final String COMMENT_ONE_LEVEL_PAGE_PREFIX = "comment:page:one:heat:";

    public static final String COMMENT_REPLY_PAGE_PREFIX = "comment:page:reply:time:";

    public static String buildHaveEarliestReplyCommentKey(Long commentId) {
        return HAVE_COMMENT_EARLIEST_REPLY_PREFIX + commentId;
    }

    public static String buildOneLevelVersionKey(Long noteId) {
        return COMMENT_ONE_LEVEL_VERSION_PREFIX + noteId;
    }

    public static String buildReplyVersionKey(Long parentId) {
        return COMMENT_REPLY_VERSION_PREFIX + parentId;
    }

    public static String buildOneLevelPageKey(Long noteId, Long version, Long cursor, Integer limit) {
        return COMMENT_ONE_LEVEL_PAGE_PREFIX + noteId + ":" + version + ":" + normalizeCursor(cursor) + ":" + limit;
    }

    public static String buildReplyPageKey(Long noteId, Long parentId, Long version, Long cursor, Integer limit) {
        return COMMENT_REPLY_PAGE_PREFIX + noteId + ":" + parentId + ":" + version + ":" + normalizeCursor(cursor) + ":" + limit;
    }

    private static String normalizeCursor(Long cursor) {
        return cursor == null ? "0" : String.valueOf(cursor);
    }
}
    
