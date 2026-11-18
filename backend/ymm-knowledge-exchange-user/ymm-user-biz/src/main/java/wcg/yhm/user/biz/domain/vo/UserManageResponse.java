package wcg.yhm.user.biz.domain.vo;

import cn.hutool.core.bean.BeanUtil;
import lombok.Data;
import wcg.yhm.user.biz.domain.po.User;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-09
 * @Description: 用户管理响应类
 * @Version: 1.0
 */

@Data
public class UserManageResponse implements Serializable {
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
    private Integer status;

    /**
     * 将user转换为 UserManageResponse
     * @param user
     */
    public static UserManageResponse copyBean(User user) {
        return BeanUtil.copyProperties(user, UserManageResponse.class);
    }
}
    

