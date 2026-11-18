package wcg.yhm.ymmknowledgeexchange.auth.enums;

import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 登录方式枚举类
 * @Version: 1.0
 */

@Getter
public enum LoginTypeEnum {
    VERIFICATION_CODE(1),
    PASSWORD(2);

    private final Integer value;

    LoginTypeEnum(Integer value) {
        this.value = value;
    }

    public static LoginTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }

        for (LoginTypeEnum loginTypeEnum : LoginTypeEnum.values()) {
            if (loginTypeEnum.getValue().equals(value)) {
                return loginTypeEnum;
            }
        }
        return null;
    }
}
    
