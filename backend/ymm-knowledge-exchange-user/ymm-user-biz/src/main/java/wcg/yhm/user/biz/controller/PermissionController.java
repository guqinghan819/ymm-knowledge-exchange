package wcg.yhm.user.biz.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.user.biz.domain.po.Permission;
import wcg.yhm.user.biz.enums.DeletedEnum;
import wcg.yhm.user.biz.service.IPermissionService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 权限表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@RestController
@RequestMapping("/permission")
@Tag(name = "权限相关接口")
public class PermissionController {

    @Resource
    private IPermissionService permissionService;

    @ApiOperationLog(description = "获取权限列表")
    @Operation(summary = "获取权限列表")
    @GetMapping("/get")
    public Response<List<Permission>> getPermissionList() {
        List<Permission> list = permissionService.lambdaQuery()
                .eq(Permission::getIsDeleted, DeletedEnum.NO.getValue())
                .orderByAsc(Permission::getSort)
                .orderByAsc(Permission::getId)
                .list();
        return Response.success(list);
    }
}
