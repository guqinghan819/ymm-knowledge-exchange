package wcg.yhm.user.biz.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleRelServiceImplTest {

    @Mock private UserRoleRelMapper relationMapper;
    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private RbacCacheService cacheService;

    private UserRoleRelServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(
                new MybatisConfiguration(), UserRoleRel.class.getName()), UserRoleRel.class);
        service = spy(new UserRoleRelServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", relationMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "rbacCacheService", cacheService);
    }

    @Test
    void assignRolesDeduplicatesInputAndRefreshesCache() {
        when(userMapper.selectById(7L)).thenReturn(availableUser(7L));
        when(roleMapper.selectCount(any())).thenReturn(2L);
        when(relationMapper.selectList(any())).thenReturn(List.of());
        doReturn(true).when(service).saveBatch(anyCollection());
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(cacheService).afterCommit(any(Runnable.class));

        service.assignRoles(7L, List.of(1L, 1L, 2L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UserRoleRel>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(service).saveBatch(captor.capture());
        assertThat(captor.getValue()).extracting(UserRoleRel::getRoleId)
                .containsExactlyInAnyOrder(1L, 2L);
        verify(cacheService).refreshUserRoles(7L);
    }

    @Test
    void assignRolesRejectsUnavailableRoleBeforeChangingRelations() {
        when(userMapper.selectById(7L)).thenReturn(availableUser(7L));
        when(roleMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.assignRoles(7L, List.of(1L, 2L)))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCodeEnum.ROLE_NOT_AVAILABLE.getErrorCode()));

        verify(relationMapper, never()).selectList(any());
        verify(cacheService, never()).afterCommit(any());
    }

    @Test
    void emptyRoleListRevokesEveryActiveRelation() {
        UserRoleRel activeRelation = new UserRoleRel().setId(20L).setUserId(7L).setRoleId(1L)
                .setIsDeleted(DeletedEnum.NO.getValue());
        when(userMapper.selectById(7L)).thenReturn(availableUser(7L));
        when(relationMapper.selectList(any())).thenReturn(List.of(activeRelation));
        doReturn(true).when(service).updateBatchById(anyCollection());
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(cacheService).afterCommit(any(Runnable.class));

        service.assignRoles(7L, List.of());

        assertThat(activeRelation.getIsDeleted()).isTrue();
        verify(service, never()).saveBatch(anyCollection());
        verify(cacheService).refreshUserRoles(7L);
    }

    private User availableUser(Long id) {
        return new User().setId(id).setStatus(StatusEnum.ENABLE.getValue())
                .setIsDelete(DeletedEnum.NO.getValue());
    }
}
