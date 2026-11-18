package wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-25
 * @Description: 用户编辑请求
 * @Version: 1.0
 */

@Data
public class UserUpdateRequest implements Serializable {

    private Long id;

    private String ymmId;

    private String nickname;

    private String newPassword;

    private LocalDate birthday;

    private String avatar;

    private String introduce;

    private Integer sex;

    private String backgroundImg;

    private String phone;

    private Integer status;

    private String code;
}
    
