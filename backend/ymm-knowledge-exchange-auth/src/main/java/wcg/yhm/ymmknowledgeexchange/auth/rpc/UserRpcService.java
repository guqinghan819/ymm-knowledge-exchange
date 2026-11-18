package wcg.yhm.ymmknowledgeexchange.auth.rpc;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.user.api.controller.UserFeignApi;
import wcg.yhm.user.api.domain.dto.GetUserByPhoneRequest;
import wcg.yhm.user.api.domain.dto.UpdatePasswordRequest;
import wcg.yhm.user.api.domain.dto.UserByPhoneResponse;
import wcg.yhm.user.api.domain.dto.UserRegisterRequest;
import wcg.yhm.ymmknowledgeexchange.auth.enums.ResponseCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-30
 * @Description:
 * @Version: 1.0
 */

@Component
@Slf4j
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 用户注册
     * @param phone 手机号
     * @return
     */
    public Long register(String phone) {
        return register(phone, null);
    }

    public Long register(String phone, String encodedPassword) {
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest();
        userRegisterRequest.setPhone(phone);
        userRegisterRequest.setEncodedPassword(encodedPassword);
        log.info("================== userFeignApi.register  =================");
        Response<Long> register = userFeignApi.register(userRegisterRequest);
        if (!register.isSuccess()) {
            return null;
        }
        return register.getData();
    }

    /**
     * 手机查询信息
     * @param phone
     * @return
     */
    public UserByPhoneResponse getByPhone(String phone) {
        GetUserByPhoneRequest getUserByPhoneRequest = new GetUserByPhoneRequest();
        getUserByPhoneRequest.setPhone(phone);
        Response<UserByPhoneResponse> byPhone = userFeignApi.getByPhone(getUserByPhoneRequest);
        if (!byPhone.isSuccess()) {
            throw new BizException(ResponseCodeEnum.LOGIN_ERROR);
        }
        return byPhone.getData();
    }

    /**
     * 更新密码
     * @param password 加密后的密码
     */
    public void updatePassword(String password) {
        UpdatePasswordRequest updatePasswordRequest = new UpdatePasswordRequest();
        updatePasswordRequest.setEncodePassword(password);
        Response<?> response = userFeignApi.updatePassword(updatePasswordRequest);
        if (!response.isSuccess()) {
            throw new BizException(ResponseCodeEnum.UPDATE_PASSWORD_FAIL);
        }
    }
}
    
