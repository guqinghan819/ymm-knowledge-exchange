package wcg.yhm.user.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.user.biz.domain.dto.RoleQueryRequest;
import wcg.yhm.user.biz.domain.dto.RoleSaveRequest;
import wcg.yhm.user.biz.domain.po.Role;
import wcg.yhm.user.biz.domain.vo.RoleResponse;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
public interface IRoleService extends IService<Role> {

    /**
     * 管理端：条件查询角色列表
     */
    List<RoleResponse> adminRoleList(RoleQueryRequest request);

    /**
     * 管理端：新增角色
     */
    void createRole(RoleSaveRequest request);

    /**
     * 管理端：编辑角色
     */
    void updateRole(RoleSaveRequest request);

    /**
     * 管理端：删除角色(逻辑删除，有用户引用时拒绝)
     */
    void deleteRole(Long id);
}
