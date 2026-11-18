package wcg.yhm.user.biz.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacCacheServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private UserRoleRelMapper userRoleRelMapper;
    @Mock private RolePermissionRelMapper rolePermissionRelMapper;

    private RbacCacheService cacheService;

    @BeforeEach
    void setUp() {
        initTableInfo(User.class);
        initTableInfo(Role.class);
        initTableInfo(Permission.class);
        initTableInfo(UserRoleRel.class);
        initTableInfo(RolePermissionRel.class);
        cacheService = new RbacCacheService();
        ReflectionTestUtils.setField(cacheService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(cacheService, "userMapper", userMapper);
        ReflectionTestUtils.setField(cacheService, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(cacheService, "permissionMapper", permissionMapper);
        ReflectionTestUtils.setField(cacheService, "userRoleRelMapper", userRoleRelMapper);
        ReflectionTestUtils.setField(cacheService, "rolePermissionRelMapper", rolePermissionRelMapper);
    }

    private void initTableInfo(Class<?> entityClass) {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), entityClass.getName()), entityClass);
    }

    @AfterEach
    void cleanTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void refreshUserRolesWritesOnlyAvailableRoles() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        User user = new User().setId(7L).setStatus(StatusEnum.ENABLE.getValue())
                .setIsDelete(DeletedEnum.NO.getValue());
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userRoleRelMapper.selectList(any())).thenReturn(List.of(
                new UserRoleRel().setUserId(7L).setRoleId(1L),
                new UserRoleRel().setUserId(7L).setRoleId(2L)));
        when(roleMapper.selectList(any())).thenReturn(List.of(
                new Role().setId(1L).setRoleKey("common_user")));

        cacheService.refreshUserRoles(7L);

        verify(valueOperations).set(RedisKeyConstant.USER_ROLES_KEY_PREFIX + "7",
                "[\"common_user\"]");
    }

    @Test
    void refreshRolePermissionsWritesDistinctAvailablePermissionKeys() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Role role = new Role().setId(3L).setRoleKey("editor")
                .setStatus(StatusEnum.ENABLE.getValue()).setIsDeleted(DeletedEnum.NO.getValue());
        when(roleMapper.selectById(3L)).thenReturn(role);
        when(rolePermissionRelMapper.selectList(any())).thenReturn(List.of(
                new RolePermissionRel().setRoleId(3L).setPermissionId(10L),
                new RolePermissionRel().setRoleId(3L).setPermissionId(11L)));
        when(permissionMapper.selectList(any())).thenReturn(List.of(
                new Permission().setId(10L).setPermissionKey("app:note:publish"),
                new Permission().setId(11L).setPermissionKey("app:note:publish")));

        cacheService.refreshRolePermissions(3L);

        verify(valueOperations).set(RedisKeyConstant.ROLE_PERMISSIONS_KEY_PREFIX + "editor",
                "[\"app:note:publish\"]");
    }

    @Test
    void refreshRunsOnlyAfterTransactionCommit() {
        AtomicBoolean refreshed = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        cacheService.afterCommit(() -> refreshed.set(true));

        org.assertj.core.api.Assertions.assertThat(refreshed).isFalse();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        org.assertj.core.api.Assertions.assertThat(refreshed).isTrue();
    }
}
