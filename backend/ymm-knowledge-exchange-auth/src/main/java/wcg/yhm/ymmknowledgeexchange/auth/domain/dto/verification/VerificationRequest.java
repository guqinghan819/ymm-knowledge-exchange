package wcg.yhm.ymmknowledgeexchange.auth.domain.dto.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import wcg.yhm.ymmknowledgeexchange.common.validator.PhoneValidator;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: 短信验证码请求
 * @Version: 1.0
 */

@Data
public class VerificationRequest {
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空, Ciallo～(∠・ω< )⌒★")
    @PhoneValidator
    private String phone;

    /**
     * 短信用途: 登陆验证-1，密码修改验证-2
     */
    @NotNull(message = "短信用途不能为空, Ciallo～(∠・ω< )⌒★")
    private Integer verificationType;
}
    
