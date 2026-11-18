package wcg.yhm.user.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.user.biz.domain.po.UserRoleRel;

import java.util.List;

/**
 * <p>
 * 用户角色表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
public interface IUserRoleRelService extends IService<UserRoleRel> {

    List<Long> getRoleIds(Long userId);

    void assignRoles(Long userId, List<Long> roleIds);
}
