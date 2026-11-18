package wcg.yhm.user.biz.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.user.biz.domain.dto.UserRoleAssignRequest;
import wcg.yhm.user.biz.domain.dto.UserRoleQueryRequest;
import wcg.yhm.user.biz.service.IUserRoleRelService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 用户角色表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@RestController
@RequestMapping("/user-role/admin")
@Tag(name = "管理端-用户角色管理")
public class UserRoleRelController {

    @Resource
    private IUserRoleRelService userRoleRelService;

    @ApiOperationLog(description = "查询用户角色")
    @Operation(summary = "查询用户当前有效角色ID")
    @PostMapping("/get")
    public Response<List<Long>> get(@Validated @RequestBody UserRoleQueryRequest request) {
        return Response.success(userRoleRelService.getRoleIds(request.getUserId()));
    }

    @ApiOperationLog(description = "分配用户角色")
    @Operation(summary = "覆盖式分配用户角色")
    @PostMapping("/assign")
    public Response<?> assign(@Validated @RequestBody UserRoleAssignRequest request) {
        userRoleRelService.assignRoles(request.getUserId(), request.getRoleIds());
        return Response.success();
    }
}
