package wcg.yhm.ymmknowledgeexchange.auth.controller;


import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user.UserLoginRequest;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user.UserUpdateRequest;
import wcg.yhm.ymmknowledgeexchange.auth.service.IUserService;

import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@RestController
@Tag(name = "用户相关接口")
@Slf4j
public class UserController {
    @Resource
    private IUserService userService;
    @PostMapping("/loginOrRegister")
    @Operation(summary = "用户登录注册")
    @ApiOperationLog(description = "用户登录注册")
    public Response<String> userLoginOrRegister(@Validated @RequestBody UserLoginRequest userLoginRequest) {
        String token = userService.userLoginOrRegister(userLoginRequest);
        return Response.success(token);
    }

    @GetMapping("/logout")
    @Operation(summary = "用户登出")
    @ApiOperationLog(description = "用户登出")
    public Response<?> userLogout() {
        Long userId = UserContextHolder.getUserId();
        log.info("==========> UserContextHolder 用户id: {}", userId);
        StpUtil.logout(userId);
        return Response.success();
    }

    @Operation(summary = "修改密码")
    @ApiOperationLog(description = "修改密码")
    @PostMapping("/update/password")
    public Response<?> updatePassword(@RequestBody UserUpdateRequest userUpdateRequest) {
        userService.updatePassword(userUpdateRequest);
        return Response.success();
    }
}
