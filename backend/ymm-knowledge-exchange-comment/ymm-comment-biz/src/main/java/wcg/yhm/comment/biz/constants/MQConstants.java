package wcg.yhm.comment.biz.constants;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-03
 * @Description: mq消息常量
 * @Version: 1.0
 */

public interface MQConstants {

    /**
     * 消费评论发布主题
     */
    String TOPIC_PUBLISH_COMMENT = "PublishCommentTopic";

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

    /**
     * Tag: 点赞
     */
    String TAG_LIKE = "Like";

    /**
     * Tag: 取消点赞
     */
    String TAG_UNLIKE = "Unlike";
}
    
