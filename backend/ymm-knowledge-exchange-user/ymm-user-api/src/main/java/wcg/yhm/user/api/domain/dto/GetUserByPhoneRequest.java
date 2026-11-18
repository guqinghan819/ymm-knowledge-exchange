package wcg.yhm.user.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import wcg.yhm.ymmknowledgeexchange.common.validator.PhoneValidator;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-30
 * @Description: 通过手机号查询用户
 * @Version: 1.0
 */

@Data
public class GetUserByPhoneRequest implements Serializable {
    /**
     * 手机号
     */
    @PhoneValidator
    @NotBlank(message = "手机号不能未空 ಠ_ಠ")
    private String phone;
}
    
