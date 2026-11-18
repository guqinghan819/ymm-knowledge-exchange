package wcg.yhm.user.biz.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

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

    private String nickname;

    private String newPassword;

    private LocalDate birthday;

    private MultipartFile avatar;

    private String introduce;

    private Integer sex;

    private MultipartFile backgroundImg;

    private String phone;

    private Integer status;

}
    
