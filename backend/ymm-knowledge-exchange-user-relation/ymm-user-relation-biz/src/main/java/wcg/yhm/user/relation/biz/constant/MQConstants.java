package wcg.yhm.user.relation.biz.constant;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description: 消息常量
 * @Version: 1.0
 */

public interface MQConstants {
    //可以理解为Topic为一级分类，tag为二级分类
    /**
     * Topic: 关注、取关共用一个
     */
    String TOPIC_FOLLOW_OR_UNFOLLOW = "FollowUnfollowTopic";

    /**
     * 关注标签
     */
    String TAG_FOLLOW = "Follow";

    /**
     * 取关标签
     */
    String TAG_UNFOLLOW = "Unfollow";

    /**
     * Topic: 关注数计数
     */
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    /**
     * Topic: 粉丝数计数
     */
    String TOPIC_COUNT_FANS = "CountFansTopic";
}
    
