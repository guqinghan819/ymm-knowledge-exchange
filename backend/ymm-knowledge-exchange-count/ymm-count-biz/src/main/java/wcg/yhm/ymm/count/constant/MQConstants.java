package wcg.yhm.ymm.count.constant;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description: 消息常量
 * @Version: 1.0
 */

public interface MQConstants {
    //可以理解为Topic为一级分类，tag为二级分类


    /**
     * Topic: 关注数计数
     */
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    /**
     * Topic: 粉丝数计数
     */
    String TOPIC_COUNT_FANS = "CountFansTopic";

    /**
     * Topic: 粉丝数计数入库
     */
    String TOPIC_COUNT_FANS_2_DB = "CountFans2DBTopic";

    /**
     * 关注
     */
    String TOPIC_COUNT_FOLLOWING_2_DB = "CountFollowing2DBTopic";

    /**
     * 点赞计数主题
     */
    String TOPIC_COUNT_NOTE_LIKE = "CountNoteLikeTopic";

    /**
     * 笔记获赞落库主题
     */
    String TOPIC_COUNT_NOTE_2_DB = "CountNoteLike2DBTopic";

    /**
     * 收藏计数主题
     */
    String TOPIC_COUNT_NOTE_COLLECT = "CountNoteCollectTopic";

    /**
     * 笔记收藏落库主题
     */
    String TOPIC_COUNT_NOTE_COLLECT_2_DB = "CountNoteCollect2DBTopic";


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

    /**
     * 点赞或者取消点赞消息
     */
    String TOPIC_NOTE_LIKE_OR_UNLIKE = "NoteLikeOrUnlike";


    /**
     * 主题: 评论计数主题
     */
    String TOPIC_COMMENT_COUNT = "CountCommentTopic";

    /**
     * Topic: 评论热度值更新
     */
    String TOPIC_COMMENT_HEAT_UPDATE = "CommentHeatUpdateTopic";

    /**
     * Topic: 评论点赞或者取消点赞
     */
    String TOPIC_COMMENT_LIKE_OR_UNLIKE = "CommentLikeOrUnlike";
}
    
