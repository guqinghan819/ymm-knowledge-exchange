package wcg.yhm.notification.biz.enums;

import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

@Getter
public enum ErrorCodeEnum implements BaseException {
    SYSTEM_ERROR("NOTIFICATION-10000", "系统异常"),
    PARAM_NOT_VALID("PARAM_NOT_VALID=20000", "参数异常"),
    USER_NOT_LOGIN("NOTIFICATION-10001", "用户未登录"),
    NOTIFICATION_NOT_FOUND("NOTIFICATION-20001", "通知不存在或无权操作");

    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
