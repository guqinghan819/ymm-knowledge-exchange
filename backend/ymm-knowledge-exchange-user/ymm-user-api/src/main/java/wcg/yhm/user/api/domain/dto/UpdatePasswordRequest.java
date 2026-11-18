package wcg.yhm.user.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-30
 * @Description: 更新密码请求
 * @Version: 1.0
 */

@Data
public class UpdatePasswordRequest implements Serializable {

    /**
     * 加密后的密码
     */
    @NotBlank(message = "不能为空哦~~~~")
    private String encodePassword;
}
    
