package wcg.yhm.user.biz.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.user.biz.domain.dto.RoleQueryRequest;
import wcg.yhm.user.biz.domain.dto.RoleSaveRequest;
import wcg.yhm.user.biz.domain.po.Role;
import wcg.yhm.user.biz.domain.po.UserRoleRel;
import wcg.yhm.user.biz.domain.vo.RoleResponse;
import wcg.yhm.user.biz.enums.DeletedEnum;
import wcg.yhm.user.biz.enums.ErrorCodeEnum;
import wcg.yhm.user.biz.enums.StatusEnum;
import wcg.yhm.user.biz.mapper.RoleMapper;
import wcg.yhm.user.biz.service.RbacCacheService;
import wcg.yhm.user.biz.service.IRoleService;
import wcg.yhm.user.biz.service.IUserRoleRelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    private static final String ADMIN_ROLE_KEY = "admin";

    @Resource
    private IUserRoleRelService userRoleRelService;

    @Resource
    private RbacCacheService rbacCacheService;

    @Override
    public List<RoleResponse> adminRoleList(RoleQueryRequest request) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getIsDeleted, DeletedEnum.NO.getValue());
        if (Objects.nonNull(request)) {
            if (StrUtil.isNotBlank(request.getRoleName())) {
                wrapper.like(Role::getRoleName, request.getRoleName());
            }
            if (StrUtil.isNotBlank(request.getRoleKey())) {
                wrapper.like(Role::getRoleKey, request.getRoleKey());
            }
            if (Objects.nonNull(request.getStatus())) {
                wrapper.eq(Role::getStatus, request.getStatus());
            }
        }
        wrapper.orderByAsc(Role::getSort).orderByAsc(Role::getId);
        List<Role> roles = list(wrapper);
        return roles.stream()
                .map(role -> BeanUtil.copyProperties(role, RoleResponse.class))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleSaveRequest request) {
        if (Objects.isNull(request) || StrUtil.isBlank(request.getRoleName()) || StrUtil.isBlank(request.getRoleKey())) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //角色名唯一
        boolean nameExists = lambdaQuery().eq(Role::getRoleName, request.getRoleName())
                .eq(Role::getIsDeleted, DeletedEnum.NO.getValue()).exists();
        if (nameExists) {
            throw new BizException(ErrorCodeEnum.ROLE_NAME_EXISTS);
        }
        //角色标识唯一(role表uk_role_key唯一索引)
        boolean keyExists = lambdaQuery().eq(Role::getRoleKey, request.getRoleKey())
                .eq(Role::getIsDeleted, DeletedEnum.NO.getValue()).exists();
        if (keyExists) {
            throw new BizException(ErrorCodeEnum.ROLE_KEY_EXISTS);
        }
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setRoleKey(request.getRoleKey());
        role.setStatus(Objects.isNull(request.getStatus()) ? StatusEnum.ENABLE.getValue() : request.getStatus());
        role.setSort(Objects.isNull(request.getSort()) ? 0 : request.getSort());
        role.setRemark(request.getRemark());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setIsDeleted(DeletedEnum.NO.getValue());
        boolean result = save(role);
        if (!result) {
            throw new BizException(ErrorCodeEnum.ROLE_CREATE_FAIL);
        }
        rbacCacheService.afterCommit(() -> rbacCacheService.refreshRolePermissions(role.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleSaveRequest request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Role role = getById(request.getId());
        if (Objects.isNull(role) || Boolean.TRUE.equals(role.getIsDeleted())) {
            throw new BizException(ErrorCodeEnum.ROLE_NOT_FOUND);
        }
        String oldRoleKey = role.getRoleKey();
        if (ADMIN_ROLE_KEY.equals(oldRoleKey)
                && ((StrUtil.isNotBlank(request.getRoleKey())
                        && !ADMIN_ROLE_KEY.equals(request.getRoleKey()))
                    || StatusEnum.DISABLED.getValue().equals(request.getStatus()))) {
            throw new BizException(ErrorCodeEnum.ADMIN_ROLE_PROTECTED);
        }
        //角色名唯一(排除自己)
        if (StrUtil.isNotBlank(request.getRoleName())) {
            boolean nameExists = lambdaQuery().eq(Role::getRoleName, request.getRoleName())
                    .eq(Role::getIsDeleted, DeletedEnum.NO.getValue())
                    .ne(Role::getId, request.getId()).exists();
            if (nameExists) {
                throw new BizException(ErrorCodeEnum.ROLE_NAME_EXISTS);
            }
        }
        //角色标识唯一(排除自己)
        if (StrUtil.isNotBlank(request.getRoleKey())) {
            boolean keyExists = lambdaQuery().eq(Role::getRoleKey, request.getRoleKey())
                    .eq(Role::getIsDeleted, DeletedEnum.NO.getValue())
                    .ne(Role::getId, request.getId()).exists();
            if (keyExists) {
                throw new BizException(ErrorCodeEnum.ROLE_KEY_EXISTS);
            }
        }
        Role update = new Role();
        update.setId(request.getId());
        if (StrUtil.isNotBlank(request.getRoleName())) {
            update.setRoleName(request.getRoleName());
        }
        if (StrUtil.isNotBlank(request.getRoleKey())) {
            update.setRoleKey(request.getRoleKey());
        }
        if (Objects.nonNull(request.getStatus())) {
            update.setStatus(request.getStatus());
        }
        if (Objects.nonNull(request.getSort())) {
            update.setSort(request.getSort());
        }
        update.setRemark(request.getRemark());
        update.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(update);
        if (!result) {
            throw new BizException(ErrorCodeEnum.ROLE_UPDATE_FAIL);
        }
        boolean roleKeyChanged = StrUtil.isNotBlank(request.getRoleKey())
                && !oldRoleKey.equals(request.getRoleKey());
        rbacCacheService.afterCommit(() -> {
            if (roleKeyChanged) {
                rbacCacheService.deleteRolePermissionCache(oldRoleKey);
            }
            rbacCacheService.refreshRolePermissions(request.getId());
            rbacCacheService.refreshUsersByRole(request.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Role role = getById(id);
        if (Objects.isNull(role) || Boolean.TRUE.equals(role.getIsDeleted())) {
            throw new BizException(ErrorCodeEnum.ROLE_NOT_FOUND);
        }
        if (ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
            throw new BizException(ErrorCodeEnum.ADMIN_ROLE_PROTECTED);
        }
        //该角色下存在用户时禁止删除
        boolean hasUsers = userRoleRelService.lambdaQuery()
                .eq(UserRoleRel::getRoleId, id)
                .eq(UserRoleRel::getIsDeleted, DeletedEnum.NO.getValue())
                .exists();
        if (hasUsers) {
            throw new BizException(ErrorCodeEnum.ROLE_HAS_USERS);
        }
        //逻辑删除角色
        Role update = new Role();
        update.setId(id);
        update.setIsDeleted(DeletedEnum.YES.getValue());
        update.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(update);
        if (!result) {
            throw new BizException(ErrorCodeEnum.ROLE_DELETE_FAIL);
        }
        rbacCacheService.afterCommit(() -> {
            rbacCacheService.deleteRolePermissionCache(role.getRoleKey());
            rbacCacheService.refreshUsersByRole(id);
        });
    }
}
