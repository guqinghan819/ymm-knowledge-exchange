package wcg.yhm.user.biz.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-07
 * @Description: 用户响应类
 * @Version: 1.0
 */

@Data
public class UserResponse implements Serializable {
    private Long id;
    private String ymmId;
    private String nickname;
    private String avatar;
    private LocalDate birthday;
    private String introduce;
    private Integer sex;
    private String backgroundImg;
    private String phone;
    private LocalDateTime createTime;
}
    
