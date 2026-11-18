package wcg.yhm.user.biz.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @Description: 管理端角色列表响应
 */
@Data
public class RoleResponse implements Serializable {

    private Long id;

    private String roleName;

    private String roleKey;

    /**
     * 状态(0：启用 1：禁用)
     */
    private Integer status;

    private Integer sort;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
