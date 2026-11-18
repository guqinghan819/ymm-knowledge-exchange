package wcg.yhm.user.api.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import wcg.yhm.user.api.constant.ServiceNameConstant;
import wcg.yhm.user.api.domain.dto.*;

import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-30
 * @Description: 用户远程调用
 * @Version: 1.0
 */

@FeignClient(name = ServiceNameConstant.SERVICE_NAME)
public interface UserFeignApi {
    String PATH_PREFIX = "/user";

    /**
     * 注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping(value = PATH_PREFIX + "/register")
    Response<Long> register(@RequestBody UserRegisterRequest userRegisterRequest);

    /**
     * 手机号获取信息
     * @param getUserByPhoneRequest
     * @return
     */
    @PostMapping(value = PATH_PREFIX + "/getByPhone")
    Response<UserByPhoneResponse> getByPhone(@Validated @RequestBody GetUserByPhoneRequest getUserByPhoneRequest);

    /**
     * 更新密码
     * @param updatePasswordRequest
     * @return
     */
    @PostMapping(value = PATH_PREFIX + "/update/password")
    Response<?> updatePassword(@Validated @RequestBody UpdatePasswordRequest updatePasswordRequest);

    /**
     * 获取创作者大致信息
     * @param queryCreatorByIdRequest
     * @return
     */
    @PostMapping(value = PATH_PREFIX + "/getCreatorById")
    Response<CreatorResponse> getCreatorById(@Validated @RequestBody QueryCreatorByIdRequest queryCreatorByIdRequest);

    /**
     * 批量获取用户信息
     * @param getUserListByIdsRequest
     * @return
     */
    @PostMapping(value = PATH_PREFIX + "/batchQueryUser")
    public Response<List<FindUserResponse>> batchQueryUser(@Validated @RequestBody GetUserListByIdsRequest getUserListByIdsRequest);
}
    
