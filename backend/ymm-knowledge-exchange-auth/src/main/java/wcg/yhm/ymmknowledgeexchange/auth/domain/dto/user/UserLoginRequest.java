package wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import wcg.yhm.ymmknowledgeexchange.common.validator.PhoneValidator;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 用户登录请求
 * @Version: 1.0
 */

@Data
public class UserLoginRequest implements Serializable {
    /**
     * 手机号
     */
    @PhoneValidator
    @NotBlank(message = "手机号不能未空 ಠ_ಠ")
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 验证码
     */
    private String code;

    /**
     * 登录类型(短信登录，密码登录)
     */
    @NotNull(message = "登录类型不能为空")
    private Integer loginType;
}
    
