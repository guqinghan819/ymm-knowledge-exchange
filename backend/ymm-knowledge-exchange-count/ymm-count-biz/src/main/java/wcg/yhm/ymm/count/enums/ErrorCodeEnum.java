package wcg.yhm.ymm.count.enums;


import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

@Getter
public enum ErrorCodeEnum implements BaseException {
    SYSTEM_ERROR("COUNT-10000", "用户关注服务异常"),
    PARAM_NOT_VALID("COUNT-10001", "参数错误"),

    ;
    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
