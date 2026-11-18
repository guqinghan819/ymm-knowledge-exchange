package wcg.yhm.user.biz.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @Description: 角色查询请求
 */
@Data
public class RoleQueryRequest implements Serializable {

    /**
     * 角色名称(模糊)
     */
    private String roleName;

    /**
     * 角色标识(模糊)
     */
    private String roleKey;

    /**
     * 状态(0：启用 1：禁用)，不传查全部
     */
    private Integer status;
}
