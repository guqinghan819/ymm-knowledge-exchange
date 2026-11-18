package wcg.yhm.comment.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description: 评论等级枚举
 * @Version: 1.0
 */

@Getter
@AllArgsConstructor
public enum CommentLevelEnum {

    // 一级评论
    ONE(1),
    // 二级评论
    TWO(2),
    ;
    private final Integer code;

}
    
