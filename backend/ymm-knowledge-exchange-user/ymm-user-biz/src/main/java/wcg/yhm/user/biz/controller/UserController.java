package wcg.yhm.user.biz.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.user.api.domain.dto.*;
import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.user.biz.domain.dto.DeleteRequest;
import wcg.yhm.user.biz.domain.dto.QueryUserDetail;
import wcg.yhm.user.biz.domain.dto.UserPageQueryRequest;
import wcg.yhm.user.biz.domain.dto.UserUpdateRequest;
import wcg.yhm.user.biz.domain.vo.UserDetailResponse;
import wcg.yhm.user.biz.domain.vo.UserManageResponse;
import wcg.yhm.user.biz.domain.vo.UserResponse;
import wcg.yhm.user.biz.service.IUserService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-28
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户相关接口")
@Slf4j
public class UserController {
    @Resource
    private IUserService userService;

    @Operation(summary = "用户信息更新")
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUser(UserUpdateRequest userUpdateRequest) {
        userService.updateUser(userUpdateRequest);
        return Response.success();
    }

    @ApiOperationLog(description = "用户注册")
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Response<Long> register(@Validated @RequestBody UserRegisterRequest userRegisterRequest) {
        Long userId = userService.userRegister(userRegisterRequest);
        return Response.success(userId);
    }

    @ApiOperationLog(description = "通过手机号获取密码")
    @Operation(summary = "通过手机号获取密码")
    @PostMapping("/getByPhone")
    public Response<UserByPhoneResponse> getByPhone(@Validated @RequestBody GetUserByPhoneRequest getUserByPhoneRequest) {
        UserByPhoneResponse byPhone = userService.getByPhone(getUserByPhoneRequest);
        return Response.success(byPhone);
    }

    @ApiOperationLog(description = "更新密码")
    @Operation(summary = "更新密码")
    @PostMapping("/update/password")
    public Response<?> updatePassword(@Validated @RequestBody UpdatePasswordRequest updatePasswordRequest) {
        userService.updatePassword(updatePasswordRequest);
        return Response.success();
    }

    @ApiOperationLog(description = "获取登陆用户")
    @Operation(summary = "获取登陆用户")
    @GetMapping("/getLoginUser")
    public Response<UserResponse> getLoginUser() {
        Long userId = UserContextHolder.getUserId();
        UserResponse loginUser = userService.getLoginUser(userId);
        return Response.success(loginUser);
    }

    @ApiOperationLog(description = "获取用户列表")
    @Operation(summary = "获取用户列表")
    @PostMapping("/getUserList")
    public Response<Page<UserManageResponse>> getAllUser(@RequestBody UserPageQueryRequest userPageQueryRequest) {
        Page<UserManageResponse> page = userService.getAllUser(userPageQueryRequest);
        return Response.success(page);
    }

    @ApiOperationLog(description = "获取创作者大致信息")
    @Operation(summary = "获取创作者大致信息")
    @PostMapping("/getCreatorById")
    public Response<CreatorResponse> getCreatorById(@Validated @RequestBody QueryCreatorByIdRequest queryCreatorByIdRequest) {
        CreatorResponse creatorById = userService.getCreatorById(queryCreatorByIdRequest);
        return Response.success(creatorById);
    }

    @ApiOperationLog(description = "批量查询用户")
    @Operation(summary = "批量查询用户")
    @PostMapping("/batchQueryUser")
    public Response<List<FindUserResponse>> batchQueryUser(@Validated @RequestBody GetUserListByIdsRequest getUserListByIdsRequest) {
        List<FindUserResponse> userListByIds = userService.getUserListByIds(getUserListByIdsRequest);
        return Response.success(userListByIds);
    }

    @ApiOperationLog(description = "根据平台id删除用户")
    @PostMapping("/delete")
    public Response<?> deleteUser(@Validated @RequestBody DeleteRequest deleteRequest) {
        userService.deleteUser(deleteRequest);
        return Response.success();
    }

    @ApiOperationLog(description = "根据平台id获取用户计数信息")
    @PostMapping("/get/detail")
    public Response<UserDetailResponse> getUserDetail(@Validated @RequestBody QueryUserDetail queryUserDetail) {
        //todo: 这里可以直接尝试从redis中获取
        UserDetailResponse userDetailResponse = userService.queryUserDetail(queryUserDetail);
        return Response.success(userDetailResponse);
    }
}
