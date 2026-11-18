package wcg.yhm.user.api.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-30
 * @Description:
 * @Version: 1.0
 */

@Data
public class UserByPhoneResponse implements Serializable {
    private Long id;
    private String password; //加密后的密码
}
    
