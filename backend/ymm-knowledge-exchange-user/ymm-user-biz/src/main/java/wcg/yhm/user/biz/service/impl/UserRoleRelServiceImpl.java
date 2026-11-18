package wcg.yhm.user.biz.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.user.biz.domain.po.Role;
import wcg.yhm.user.biz.domain.po.User;
import wcg.yhm.user.biz.domain.po.UserRoleRel;
import wcg.yhm.user.biz.enums.DeletedEnum;
import wcg.yhm.user.biz.enums.ErrorCodeEnum;
import wcg.yhm.user.biz.enums.StatusEnum;
import wcg.yhm.user.biz.mapper.RoleMapper;
import wcg.yhm.user.biz.mapper.UserMapper;
import wcg.yhm.user.biz.mapper.UserRoleRelMapper;
import wcg.yhm.user.biz.service.RbacCacheService;
import wcg.yhm.user.biz.service.IUserRoleRelService;
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
 * 用户角色表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@Service
public class UserRoleRelServiceImpl extends ServiceImpl<UserRoleRelMapper, UserRoleRel> implements IUserRoleRelService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RbacCacheService rbacCacheService;

    @Override
    public List<Long> getRoleIds(Long userId) {
        requireUser(userId);
        List<Long> relatedRoleIds = getBaseMapper().selectList(new LambdaQueryWrapper<UserRoleRel>()
                        .select(UserRoleRel::getRoleId)
                        .eq(UserRoleRel::getUserId, userId)
                        .eq(UserRoleRel::getIsDeleted, DeletedEnum.NO.getValue()))
                .stream().map(UserRoleRel::getRoleId).distinct().toList();
        if (relatedRoleIds.isEmpty()) {
            return List.of();
        }
        Set<Long> availableRoleIds = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .select(Role::getId)
                        .in(Role::getId, relatedRoleIds)
                        .eq(Role::getStatus, StatusEnum.ENABLE.getValue())
                        .eq(Role::getIsDeleted, DeletedEnum.NO.getValue()))
                .stream().map(Role::getId).collect(Collectors.toSet());
        return relatedRoleIds.stream().filter(availableRoleIds::contains).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        requireUser(userId);
        LinkedHashSet<Long> targetRoleIds = normalizeIds(roleIds);
        validateAvailableRoles(targetRoleIds);

        LocalDateTime now = LocalDateTime.now();
        List<UserRoleRel> existingRelations = getBaseMapper().selectList(
                new LambdaQueryWrapper<UserRoleRel>().eq(UserRoleRel::getUserId, userId));
        List<UserRoleRel> activeRelations = existingRelations.stream()
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

        Map<Long, UserRoleRel> reusableRelations = existingRelations.stream()
                .collect(Collectors.toMap(UserRoleRel::getRoleId, Function.identity(), (first, ignored) -> first));
        List<UserRoleRel> relationsToUpdate = new ArrayList<>();
        List<UserRoleRel> relationsToInsert = new ArrayList<>();
        targetRoleIds.forEach(roleId -> {
            UserRoleRel relation = reusableRelations.get(roleId);
            if (relation == null) {
                relation = new UserRoleRel();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
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
        rbacCacheService.afterCommit(() -> rbacCacheService.refreshUserRoles(userId));
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        User user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getIsDelete())) {
            throw new BizException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        return user;
    }

    private LinkedHashSet<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        return new LinkedHashSet<>(ids);
    }

    private void validateAvailableRoles(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        long availableCount = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .in(Role::getId, roleIds)
                .eq(Role::getStatus, StatusEnum.ENABLE.getValue())
                .eq(Role::getIsDeleted, DeletedEnum.NO.getValue()));
        if (availableCount != roleIds.size()) {
            throw new BizException(ErrorCodeEnum.ROLE_NOT_AVAILABLE);
        }
    }
}
