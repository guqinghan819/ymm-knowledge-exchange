package wcg.yhm.ymm.search.biz.index;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description: 笔记索引
 * @Version: 1.0
 */

public interface NoteIndex {

    /**
     * 索引名称
     */
    String NAME = "note";

    /**
     * 笔记ID
     */
    String FIELD_NOTE_ID = "id";

    /**
     * 封面
     */
    String FIELD_NOTE_COVER = "cover";

    /**
     * 头像
     */
    String FIELD_NOTE_TITLE = "title";

    /**
     * 话题名称
     */
    String FIELD_NOTE_TOPIC = "topic";

    /**
     * 发布者昵称
     */
    String FIELD_NOTE_NICKNAME = "nickname";

    /**
     * 发布者头像
     */
    String FIELD_NOTE_AVATAR = "avatar";

    /**
     * 笔记类型
     */
    String FIELD_NOTE_TYPE = "type";

    /**
     * 发布时间
     */
    String FIELD_NOTE_CREATE_TIME = "create_time";

    /**
     * 更新时间
     */
    String FIELD_NOTE_UPDATE_TIME = "update_time";

    /**
     * 笔记被点赞数
     */
    String FIELD_NOTE_LIKE_TOTAL = "like_total";

    /**
     * 笔记被收藏数
     */
    String FIELD_NOTE_COLLECT_TOTAL = "collect_total";

    /**
     * 笔记被评论数
     */
    String FIELD_NOTE_COMMENT_TOTAL = "comment_total";
}
    
