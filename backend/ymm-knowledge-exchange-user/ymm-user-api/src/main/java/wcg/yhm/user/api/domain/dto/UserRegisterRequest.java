package wcg.yhm.user.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import wcg.yhm.ymmknowledgeexchange.common.validator.PhoneValidator;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 用户登录请求
 * @Version: 1.0
 */

@Data
public class UserRegisterRequest implements Serializable {
    /**
     * 手机号
     */
    @PhoneValidator
    @NotBlank(message = "手机号不能未空 ಠ_ಠ")
    private String phone;

    /** 密码登录注册时传入的加密密码；验证码注册时为空。 */
    private String encodedPassword;

}
    
