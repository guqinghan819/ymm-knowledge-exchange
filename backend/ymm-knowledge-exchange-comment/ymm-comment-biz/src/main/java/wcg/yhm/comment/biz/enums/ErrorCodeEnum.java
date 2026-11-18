package wcg.yhm.comment.biz.enums;

import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-27
 * @Description: 错误码枚举
 * @Version: 1.0
 */

@Getter
public enum ErrorCodeEnum implements BaseException {
    SYSTEM_ERROR("COMMENT-10000", "评论服务系统异常 ( ´•̥×•̥` ) "),
    PARAM_NOT_VALID("COMMENT-10001", "参数错误, Ciallo～(∠・ω< )⌒★"),
    COMMENT_NOT_FOUND("COMMENT-10002", "评论不存在"),
    COMMENT_ALREADY_LIKE("COMMENT-10003", "评论已经点赞"),
    COMMENT_NOT_LIKE("COMMENT-10004", "评论未点赞"),
    USER_NOT_LOGIN("COMMENT-10005", "用户未登录");
    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
    
