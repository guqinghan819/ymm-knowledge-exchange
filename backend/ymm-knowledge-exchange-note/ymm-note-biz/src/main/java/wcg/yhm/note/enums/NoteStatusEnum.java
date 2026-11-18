package wcg.yhm.note.enums;


import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-23
 * @Description: 笔记审核状态枚举
 * @Version: 1.0
 */

@Getter
public enum NoteStatusEnum {

    PENDING_REVIEW(0), // 待审核
    NORMAL(1), // 正常
    DELETED(2), // 删除
    DOWNED(3), // 下架
    ;

    private final Integer value;

    NoteStatusEnum(Integer value) {
        this.value = value;
    }
}
