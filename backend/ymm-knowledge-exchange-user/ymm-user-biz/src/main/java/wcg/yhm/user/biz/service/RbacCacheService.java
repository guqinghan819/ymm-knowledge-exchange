package wcg.yhm.user.biz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import wcg.yhm.user.biz.constant.RedisKeyConstant;
import wcg.yhm.user.biz.domain.po.Permission;
import wcg.yhm.user.biz.domain.po.Role;
import wcg.yhm.user.biz.domain.po.RolePermissionRel;
import wcg.yhm.user.biz.domain.po.User;
import wcg.yhm.user.biz.domain.po.UserRoleRel;
import wcg.yhm.user.biz.enums.DeletedEnum;
import wcg.yhm.user.biz.enums.StatusEnum;
import wcg.yhm.user.biz.mapper.PermissionMapper;
import wcg.yhm.user.biz.mapper.RoleMapper;
import wcg.yhm.user.biz.mapper.RolePermissionRelMapper;
import wcg.yhm.user.biz.mapper.UserMapper;
import wcg.yhm.user.biz.mapper.UserRoleRelMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RbacCacheService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PermissionMapper permissionMapper;
    @Resource
    private UserRoleRelMapper userRoleRelMapper;
    @Resource
    private RolePermissionRelMapper rolePermissionRelMapper;

    public void synchronizeAll() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId)
                .eq(User::getStatus, StatusEnum.ENABLE.getValue())
                .eq(User::getIsDelete, DeletedEnum.NO.getValue()));
        List<Role> roles = getAvailableRoles();
        List<Permission> permissions = getAvailablePermissions();

        Map<Long, String> roleKeys = roles.stream()
                .collect(Collectors.toMap(Role::getId, Role::getRoleKey));
        Map<Long, String> permissionKeys = permissions.stream()
                .filter(permission -> permission.getPermissionKey() != null
                        && !permission.getPermissionKey().isBlank())
                .collect(Collectors.toMap(Permission::getId, Permission::getPermissionKey));

        Map<Long, LinkedHashSet<String>> rolesByUser = users.stream()
                .collect(Collectors.toMap(User::getId, user -> new LinkedHashSet<>()));
        if (!rolesByUser.isEmpty() && !roleKeys.isEmpty()) {
            userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                            .select(UserRoleRel::getUserId, UserRoleRel::getRoleId)
                            .in(UserRoleRel::getUserId, rolesByUser.keySet())
                            .in(UserRoleRel::getRoleId, roleKeys.keySet())
                            .eq(UserRoleRel::getIsDeleted, DeletedEnum.NO.getValue()))
                    .forEach(relation -> rolesByUser.get(relation.getUserId())
                            .add(roleKeys.get(relation.getRoleId())));
        }

        Map<Long, LinkedHashSet<String>> permissionsByRole = roles.stream()
                .collect(Collectors.toMap(Role::getId, role -> new LinkedHashSet<>()));
        if (!permissionsByRole.isEmpty() && !permissionKeys.isEmpty()) {
            rolePermissionRelMapper.selectList(new LambdaQueryWrapper<RolePermissionRel>()
                            .select(RolePermissionRel::getRoleId, RolePermissionRel::getPermissionId)
                            .in(RolePermissionRel::getRoleId, permissionsByRole.keySet())
                            .in(RolePermissionRel::getPermissionId, permissionKeys.keySet())
                            .eq(RolePermissionRel::getIsDeleted, DeletedEnum.NO.getValue()))
                    .forEach(relation -> permissionsByRole.get(relation.getRoleId())
                            .add(permissionKeys.get(relation.getPermissionId())));
        }

        Map<String, Object> userRoleCache = new HashMap<>();
        rolesByUser.forEach((userId, roleSet) -> userRoleCache.put(
                RedisKeyConstant.USER_ROLES_KEY_PREFIX + userId,
                JsonUtil.toJsonString(roleSet)));
        Map<String, Object> rolePermissionCache = new HashMap<>();
        roles.forEach(role -> rolePermissionCache.put(
                RedisKeyConstant.ROLE_PERMISSIONS_KEY_PREFIX + role.getRoleKey(),
                JsonUtil.toJsonString(permissionsByRole.get(role.getId()))));

        replacePrefix(RedisKeyConstant.USER_ROLES_KEY_PREFIX, userRoleCache);
        replacePrefix(RedisKeyConstant.ROLE_PERMISSIONS_KEY_PREFIX, rolePermissionCache);
        log.info("RBAC cache synchronized: {} users, {} roles", users.size(), roles.size());
    }

    public void refreshUserRoles(Long userId) {
        User user = userMapper.selectById(userId);
        String cacheKey = RedisKeyConstant.USER_ROLES_KEY_PREFIX + userId;
        if (user == null || Boolean.TRUE.equals(user.getIsDelete())
                || StatusEnum.DISABLED.getValue().equals(user.getStatus())) {
            redisTemplate.delete(cacheKey);
            return;
        }

        List<Long> roleIds = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                        .select(UserRoleRel::getRoleId)
                        .eq(UserRoleRel::getUserId, userId)
                        .eq(UserRoleRel::getIsDeleted, DeletedEnum.NO.getValue()))
                .stream().map(UserRoleRel::getRoleId).distinct().toList();
        List<String> roleKeys = Collections.emptyList();
        if (!roleIds.isEmpty()) {
            roleKeys = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                            .select(Role::getRoleKey)
                            .in(Role::getId, roleIds)
                            .eq(Role::getStatus, StatusEnum.ENABLE.getValue())
                            .eq(Role::getIsDeleted, DeletedEnum.NO.getValue()))
                    .stream().map(Role::getRoleKey).filter(Objects::nonNull).distinct().toList();
        }
        redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJsonString(roleKeys));
    }

    public void refreshRolePermissions(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return;
        }
        String cacheKey = RedisKeyConstant.ROLE_PERMISSIONS_KEY_PREFIX + role.getRoleKey();
        if (Boolean.TRUE.equals(role.getIsDeleted())
                || StatusEnum.DISABLED.getValue().equals(role.getStatus())) {
            redisTemplate.delete(cacheKey);
            return;
        }

        List<Long> permissionIds = rolePermissionRelMapper.selectList(
                        new LambdaQueryWrapper<RolePermissionRel>()
                                .select(RolePermissionRel::getPermissionId)
                                .eq(RolePermissionRel::getRoleId, roleId)
                                .eq(RolePermissionRel::getIsDeleted, DeletedEnum.NO.getValue()))
                .stream().map(RolePermissionRel::getPermissionId).distinct().toList();
        List<String> permissionKeys = Collections.emptyList();
        if (!permissionIds.isEmpty()) {
            permissionKeys = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                            .select(Permission::getPermissionKey)
                            .in(Permission::getId, permissionIds)
                            .eq(Permission::getStatus, StatusEnum.ENABLE.getValue())
                            .eq(Permission::getIsDeleted, DeletedEnum.NO.getValue()))
                    .stream().map(Permission::getPermissionKey)
                    .filter(key -> key != null && !key.isBlank()).distinct().toList();
        }
        redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJsonString(permissionKeys));
    }

    public void refreshUsersByRole(Long roleId) {
        userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                        .select(UserRoleRel::getUserId)
                        .eq(UserRoleRel::getRoleId, roleId)
                        .eq(UserRoleRel::getIsDeleted, DeletedEnum.NO.getValue()))
                .stream().map(UserRoleRel::getUserId).distinct()
                .forEach(this::refreshUserRoles);
    }

    public void deleteRolePermissionCache(String roleKey) {
        if (roleKey != null && !roleKey.isBlank()) {
            redisTemplate.delete(RedisKeyConstant.ROLE_PERMISSIONS_KEY_PREFIX + roleKey);
        }
    }

    public void afterCommit(Runnable cacheRefresh) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheRefresh.run();
                }
            });
            return;
        }
        cacheRefresh.run();
    }

    private List<Role> getAvailableRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .select(Role::getId, Role::getRoleKey)
                .eq(Role::getStatus, StatusEnum.ENABLE.getValue())
                .eq(Role::getIsDeleted, DeletedEnum.NO.getValue()));
    }

    private List<Permission> getAvailablePermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .select(Permission::getId, Permission::getPermissionKey)
                .eq(Permission::getStatus, StatusEnum.ENABLE.getValue())
                .eq(Permission::getIsDeleted, DeletedEnum.NO.getValue()));
    }

    private void replacePrefix(String prefix, Map<String, Object> desiredValues) {
        Set<String> staleKeys = scanKeys(prefix);
        staleKeys.removeAll(desiredValues.keySet());
        if (!staleKeys.isEmpty()) {
            redisTemplate.delete(staleKeys);
        }
        if (!desiredValues.isEmpty()) {
            redisTemplate.opsForValue().multiSet(desiredValues);
        }
    }

    private Set<String> scanKeys(String prefix) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(1000).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }
}
