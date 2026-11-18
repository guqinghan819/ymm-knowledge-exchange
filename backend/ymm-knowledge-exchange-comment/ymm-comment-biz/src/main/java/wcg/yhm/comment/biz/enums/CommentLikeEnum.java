package wcg.yhm.comment.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论点赞状态
 */
@AllArgsConstructor
@Getter
public enum CommentLikeEnum {

    LIKE(1),
    UNLIKE(0);

    private final Integer status;

    public static CommentLikeEnum getEnum(Integer status) {
        for (CommentLikeEnum value : CommentLikeEnum.values()) {
            if (value.status.equals(status)) {
                return value;
            }
        }
        return null;
    }
}
