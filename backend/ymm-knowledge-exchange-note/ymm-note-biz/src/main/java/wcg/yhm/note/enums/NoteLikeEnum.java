package wcg.yhm.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 笔记点赞状态枚举
 */

@AllArgsConstructor
@Getter
public enum NoteLikeEnum {
    //点赞
    LIKE(1),
    //取消点赞
    UNLIKE(0),
    ;
    private final Integer status;
}
