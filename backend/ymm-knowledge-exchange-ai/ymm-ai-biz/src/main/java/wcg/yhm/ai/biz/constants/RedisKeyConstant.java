package wcg.yhm.ai.biz.constants;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: redis的key常量类
 * @Version: 1.0
 */

public interface RedisKeyConstant {
    //笔记信息缓存key前缀
    String NOTE_DETAIL_KEY_PREFIX = "note:detail:";

    //布隆过滤器用户点赞列表前缀
    String BLOOM_USER_NOTE_LIKE_LIST_KEY = "bloom:note:likes:";

    //zset用户点赞列表前缀
    String ZSET_USER_NOTE_LIKE_LIKE_KEY = "zset:note:likes:";

    //bloom用户收藏列表前缀
    String BLOOM_USER_NOTE_COLLECT_LIST_KEY = "bloom:note:collect:";

    String ZSET_USER_NOTE_COLLECT_KEY = "zset:note:collect:";

    //频道key
    String CHANNEL_KEY = "channel";

    //话题key
    String TOPIC_CHANNEL_KEY_PREFIX = "topic:channel:";
}
    
