package wcg.yhm.comment.biz.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import wcg.yhm.comment.biz.domain.dto.CommentPageCacheDTO;

import java.util.concurrent.TimeUnit;

/**
 * 评论列表本地缓存。
 */
public class CommentCacheUtil {

    private static final Cache<String, CommentPageCacheDTO> COMMENT_PAGE_CACHE =
            Caffeine.newBuilder()
                    .initialCapacity(1000)
                    .maximumSize(10000)
                    .expireAfterWrite(30, TimeUnit.SECONDS)
                    .build();

    private CommentCacheUtil() {
    }

    public static CommentPageCacheDTO getCommentPage(String key) {
        return COMMENT_PAGE_CACHE.getIfPresent(key);
    }

    public static void saveCommentPage(String key, CommentPageCacheDTO page) {
        COMMENT_PAGE_CACHE.put(key, page);
    }
}
