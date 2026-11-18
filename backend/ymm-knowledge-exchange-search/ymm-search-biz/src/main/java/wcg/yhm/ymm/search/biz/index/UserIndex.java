package wcg.yhm.ymm.search.biz.index;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description: 用户索引
 * @Version: 1.0
 */

public interface UserIndex {

    /**
     * 索引名称
     */
    String NAME = "user";

    String FIELD_USER_ID = "id";

    String FIELD_USER_YMM_ID = "ymm_id";

    String FIELD_USER_NICKNAME = "nickname";

    String FIELD_USER_AVATAR = "avatar";

    String FIELD_USER_NOTE_TOTAL = "note_total";

    String FIELD_USER_FANS_TOTAL = "fans_total";
}
    
