package wcg.yhm.ymm.search.biz.enums;

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
    SYSTEM_ERROR("SEARCH-10000", "搜索服务系统异常 ( ´•̥×•̥` ) "),
    PARAM_NOT_VALID("SEARCH-10001", "参数错误, Ciallo～(∠・ω< )⌒★"),
    SORT_ENUM_NOT_EXISTS("SEARCH-10002", "排序字段不存在"),
    ;
    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
    
