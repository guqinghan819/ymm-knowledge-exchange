package wcg.yhm.user.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.user.biz.domain.po.RolePermissionRel;

import java.util.List;

/**
 * <p>
 * 用户权限表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
public interface IRolePermissionRelService extends IService<RolePermissionRel> {

    List<Long> getPermissionIds(Long roleId);

    void assignPermissions(Long roleId, List<Long> permissionIds);
}
