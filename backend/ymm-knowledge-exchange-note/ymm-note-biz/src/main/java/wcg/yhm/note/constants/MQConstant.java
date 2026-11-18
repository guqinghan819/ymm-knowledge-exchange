package wcg.yhm.note.constants;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-24
 * @Description: 消息队列常量
 * @Version: 1.0
 */

public interface MQConstant {

    /**
     * topic 主题: 删除本地缓存
     */
    String TOPIC_DELETE_NOTE_LOCAL_CACHE = "DeleteNoteLocalCacheTopic";

    /**
     * 延迟删除redis中的note缓存
     */
    String TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE = "DelayDeleteNoteRedisCacheTopic";

    /**
     * 点赞或者取消点赞消息
     */
    String TOPIC_NOTE_LIKE_OR_UNLIKE = "NoteLikeOrUnlike";

    /**
     * 点赞标签
     */
    String TAG_LIKE = "Like";

    /**
     * Tag 标签：取消点赞
     */
    String TAG_UNLIKE = "Unlike";

    /**
     * 点赞计数主题
     */
    String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";


    /**
     * 收藏或者取消收藏消息
     */
    String TOPIC_NOTE_COLLECT_OR_UN_COLLECT = "NoteCollectOrUnCollect";

    /**
     * 收藏标签
     */
    String TAG_COLLECT = "Collect";

    /**
     * Tag 标签：取消收藏
     */
    String TAG_UN_COLLECT = "UnCollect";

    /**
     * 收藏计数主题
     */
    String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic";

    /**
     * 发布笔记计数主题
     */
    String TOPIC_COUNT_NOTE_PUBLISH = "CountNotePublishTopic";

    /**
     * 发布标签
     */
    String TAG_PUBLISH = "Publish";

    /**
     * 删除笔记标签
     */
    String TAG_DELETE = "Delete";

    /**
     * 主题: 当笔记审核通过后,在note_count表中插入数据
     */
    String TOPIC_INSERT_NOTE_COUNT_RECORD = "InsertNoteCountRecord";


}
    
