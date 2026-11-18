package wcg.yhm.user.biz.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @Description: 角色新增/修改请求
 */
@Data
public class RoleSaveRequest implements Serializable {

    /**
     * 角色id(新增为空)
     */
    private Long id;

    private String roleName;

    private String roleKey;

    /**
     * 状态(0：启用 1：禁用)
     */
    private Integer status;

    /**
     * 显示顺序
     */
    private Integer sort;

    private String remark;
}
