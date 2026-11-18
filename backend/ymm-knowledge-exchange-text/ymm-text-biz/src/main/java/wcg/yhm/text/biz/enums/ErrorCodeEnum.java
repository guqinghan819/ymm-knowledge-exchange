package wcg.yhm.text.biz.enums;

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
    SYSTEM_ERROR("TEXT-10000", "文本存储服务系统异常 ( ´•̥×•̥` ) "),
    PARAM_NOT_VALID("TEXT-10001", "参数错误, Ciallo～(∠・ω< )⌒★"),
    OPERATION_FAIL("TEXT-20001", "操作失败 ( ´•̥×•̥` ) "),
    CONTENT_NOT_EXIST("TEXT-20003", "内容不存在 Ciallo～(∠・ω< )⌒★");
    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
    
