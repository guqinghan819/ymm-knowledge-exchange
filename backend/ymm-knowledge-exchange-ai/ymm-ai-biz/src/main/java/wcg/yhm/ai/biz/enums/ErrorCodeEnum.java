package wcg.yhm.ai.biz.enums;

import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-23
 * @Description: 错误码枚举
 * @Version: 1.0
 */

@Getter
public enum ErrorCodeEnum implements BaseException {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("AI-10000", "ai服务异常"),
    PARAM_NOT_VALID("AI-10001", "参数错误"),


    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
    
