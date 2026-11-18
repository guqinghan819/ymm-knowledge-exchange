package wcg.yhm.user.biz.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.user.biz.domain.po.Permission;
import wcg.yhm.user.biz.domain.po.Role;
import wcg.yhm.user.biz.domain.po.RolePermissionRel;
import wcg.yhm.user.biz.enums.DeletedEnum;
import wcg.yhm.user.biz.enums.ErrorCodeEnum;
import wcg.yhm.user.biz.enums.StatusEnum;
import wcg.yhm.user.biz.mapper.PermissionMapper;
import wcg.yhm.user.biz.mapper.RoleMapper;
import wcg.yhm.user.biz.mapper.RolePermissionRelMapper;
import wcg.yhm.user.biz.service.IRolePermissionRelService;
import wcg.yhm.user.biz.service.RbacCacheService;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户权限表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@Service
public class RolePermissionRelServiceImpl extends ServiceImpl<RolePermissionRelMapper, RolePermissionRel> implements IRolePermissionRelService {

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PermissionMapper permissionMapper;
    @Resource
    private RbacCacheService rbacCacheService;

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        requireAvailableRole(roleId);
        List<Long> relatedPermissionIds = getBaseMapper().selectList(
                        new LambdaQueryWrapper<RolePermissionRel>()
                                .select(RolePermissionRel::getPermissionId)
                                .eq(RolePermissionRel::getRoleId, roleId)
                                .eq(RolePermissionRel::getIsDeleted, DeletedEnum.NO.getValue()))
                .stream().map(RolePermissionRel::getPermissionId).distinct().toList();
        if (relatedPermissionIds.isEmpty()) {
            return List.of();
        }
        Set<Long> availablePermissionIds = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                        .select(Permission::getId)
                        .in(Permission::getId, relatedPermissionIds)
                        .eq(Permission::getStatus, StatusEnum.ENABLE.getValue())
                        .eq(Permission::getIsDeleted, DeletedEnum.NO.getValue()))
                .stream().map(Permission::getId).collect(Collectors.toSet());
        return relatedPermissionIds.stream().filter(availablePermissionIds::contains).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        requireAvailableRole(roleId);
        LinkedHashSet<Long> targetPermissionIds = normalizeIds(permissionIds);
        validateAvailablePermissions(targetPermissionIds);

        LocalDateTime now = LocalDateTime.now();
        List<RolePermissionRel> existingRelations = getBaseMapper().selectList(
                new LambdaQueryWrapper<RolePermissionRel>().eq(RolePermissionRel::getRoleId, roleId));
        List<RolePermissionRel> activeRelations = existingRelations.stream()
                .filter(relation -> !Boolean.TRUE.equals(relation.getIsDeleted()))
                .toList();
        if (!activeRelations.isEmpty()) {
            activeRelations.forEach(relation -> {
                relation.setIsDeleted(DeletedEnum.YES.getValue());
                relation.setUpdateTime(now);
            });
            if (!updateBatchById(activeRelations)) {
                throw new BizException(ErrorCodeEnum.RBAC_RELATION_UPDATE_FAIL);
            }
        }

        Map<Long, RolePermissionRel> reusableRelations = existingRelations.stream()
                .collect(Collectors.toMap(RolePermissionRel::getPermissionId,
                        Function.identity(), (first, ignored) -> first));
        List<RolePermissionRel> relationsToUpdate = new ArrayList<>();
        List<RolePermissionRel> relationsToInsert = new ArrayList<>();
        targetPermissionIds.forEach(permissionId -> {
            RolePermissionRel relation = reusableRelations.get(permissionId);
            if (relation == null) {
                relation = new RolePermissionRel();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                relation.setCreateTime(now);
                relationsToInsert.add(relation);
            } else {
                relationsToUpdate.add(relation);
            }
            relation.setUpdateTime(now);
            relation.setIsDeleted(DeletedEnum.NO.getValue());
        });
        if (!relationsToUpdate.isEmpty() && !updateBatchById(relationsToUpdate)) {
            throw new BizException(ErrorCodeEnum.RBAC_RELATION_UPDATE_FAIL);
        }
        if (!relationsToInsert.isEmpty() && !saveBatch(relationsToInsert)) {
            throw new BizException(ErrorCodeEnum.RBAC_RELATION_UPDATE_FAIL);
        }
        rbacCacheService.afterCommit(() -> rbacCacheService.refreshRolePermissions(roleId));
    }

    private Role requireAvailableRole(Long roleId) {
        if (roleId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null || Boolean.TRUE.equals(role.getIsDeleted())
                || StatusEnum.DISABLED.getValue().equals(role.getStatus())) {
            throw new BizException(ErrorCodeEnum.ROLE_NOT_AVAILABLE);
        }
        return role;
    }

    private LinkedHashSet<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        return new LinkedHashSet<>(ids);
    }

    private void validateAvailablePermissions(Set<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return;
        }
        long availableCount = permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                .in(Permission::getId, permissionIds)
                .eq(Permission::getStatus, StatusEnum.ENABLE.getValue())
                .eq(Permission::getIsDeleted, DeletedEnum.NO.getValue()));
        if (availableCount != permissionIds.size()) {
            throw new BizException(ErrorCodeEnum.PERMISSION_NOT_AVAILABLE);
        }
    }
}
