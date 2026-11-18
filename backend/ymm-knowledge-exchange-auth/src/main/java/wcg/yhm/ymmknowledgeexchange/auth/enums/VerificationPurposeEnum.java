package wcg.yhm.ymmknowledgeexchange.auth.enums;

import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-25
 * @Description: 短信验证码用途枚举
 * @Version: 1.0
 */

@Getter
public enum VerificationPurposeEnum {
    USER_LOGIN(1),
    UPDATE_PASSWORD(2),
    UPDATE_PHONE(3);

    private final Integer value;

     VerificationPurposeEnum(Integer value) {
        this.value = value;
    }

    public static VerificationPurposeEnum getByValue(Integer value) {
        if (value == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }

        for (VerificationPurposeEnum purposeEnum : VerificationPurposeEnum.values()) {
            if (purposeEnum.getValue().equals(value)) {
                return purposeEnum;
            }
        }
        return null;
    }
}
    
