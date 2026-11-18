package wcg.yhm.ymmknowledgeexchange.auth.service;

import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user.UserLoginRequest;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user.UserUpdateRequest;
import wcg.yhm.ymmknowledgeexchange.auth.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
public interface IUserService extends IService<User> {

    /**
     * 用户登录注册
     * @return
     */
    String userLoginOrRegister(UserLoginRequest userLoginRequest);

    /**
     * 修改密码
     * @param userUpdateRequest
     */
    void updatePassword(UserUpdateRequest userUpdateRequest);
}
