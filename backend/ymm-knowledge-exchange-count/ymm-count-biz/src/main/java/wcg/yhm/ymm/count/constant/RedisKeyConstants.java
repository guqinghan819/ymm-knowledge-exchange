package wcg.yhm.ymm.count.constant;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-03
 * @Description: redis键常量
 * @Version: 1.0
 */

public interface RedisKeyConstants {

    /**
     * 用户维度计数 Key 前缀
     */
    String COUNT_USER_KEY_PREFIX = "count:user:";

    /**
     * Hash Field: 粉丝总数
     */
    String FIELD_FANS_TOTAL = "fansTotal";

    /**
     * Hash Field: 关注总数
     */
    String FIELD_FOLLOWING_TOTAL = "followingTotal";

    /**
     * 笔记维度计数 key 前缀
     */
    String COUNT_NOTE_KEY_PREFIX = "count:note:";

    /**
     * hash field: 笔记获赞总数
     */
    String FIELD_LIKE_TOTAL = "likeTotal";

    /**
     * hash field: 笔记收藏总数
     */
    String FIELD_COLLECTION_TOTAL = "collectionTotal";

    /**
     * hash field: 发布总数
     */
    String FIELD_PUBLISH_TOTAL = "publicTotal";
}
    
