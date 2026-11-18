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
import wcg.yhm.user.biz.domain.dto.IdRequest;
import wcg.yhm.user.biz.domain.dto.RolePermissionAssignRequest;
import wcg.yhm.user.biz.domain.dto.RolePermissionQueryRequest;
import wcg.yhm.user.biz.domain.dto.RoleQueryRequest;
import wcg.yhm.user.biz.domain.dto.RoleSaveRequest;
import wcg.yhm.user.biz.domain.vo.RoleResponse;
import wcg.yhm.user.biz.service.IRoleService;
import wcg.yhm.user.biz.service.IRolePermissionRelService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 管理端：角色管理接口
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@RestController
@RequestMapping("/role/admin")
@Tag(name = "管理端-角色管理")
public class RoleAdminController {

    @Resource
    private IRoleService roleService;

    @Resource
    private IRolePermissionRelService rolePermissionRelService;

    @ApiOperationLog(description = "管理端查询角色列表")
    @Operation(summary = "管理端查询角色列表")
    @PostMapping("/list")
    public Response<List<RoleResponse>> list(@RequestBody RoleQueryRequest request) {
        return Response.success(roleService.adminRoleList(request));
    }

    @ApiOperationLog(description = "管理端新增角色")
    @Operation(summary = "管理端新增角色")
    @PostMapping("/create")
    public Response<?> create(@RequestBody RoleSaveRequest request) {
        roleService.createRole(request);
        return Response.success();
    }

    @ApiOperationLog(description = "管理端编辑角色")
    @Operation(summary = "管理端编辑角色")
    @PostMapping("/update")
    public Response<?> update(@RequestBody RoleSaveRequest request) {
        roleService.updateRole(request);
        return Response.success();
    }

    @ApiOperationLog(description = "管理端删除角色")
    @Operation(summary = "管理端删除角色")
    @PostMapping("/delete")
    public Response<?> delete(@Validated @RequestBody IdRequest request) {
        roleService.deleteRole(request.getId());
        return Response.success();
    }

    @ApiOperationLog(description = "查询角色权限")
    @Operation(summary = "查询角色当前有效权限ID")
    @PostMapping("/permissions/get")
    public Response<List<Long>> getPermissions(
            @Validated @RequestBody RolePermissionQueryRequest request) {
        return Response.success(rolePermissionRelService.getPermissionIds(request.getRoleId()));
    }

    @ApiOperationLog(description = "分配角色权限")
    @Operation(summary = "覆盖式分配角色权限")
    @PostMapping("/permissions/assign")
    public Response<?> assignPermissions(
            @Validated @RequestBody RolePermissionAssignRequest request) {
        rolePermissionRelService.assignPermissions(request.getRoleId(), request.getPermissionIds());
        return Response.success();
    }
}
