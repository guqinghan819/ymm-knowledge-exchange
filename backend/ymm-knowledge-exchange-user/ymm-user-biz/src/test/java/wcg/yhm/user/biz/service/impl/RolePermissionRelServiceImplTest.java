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
import wcg.yhm.user.biz.domain.po.RolePermissionRel;
import wcg.yhm.user.biz.enums.DeletedEnum;
import wcg.yhm.user.biz.enums.StatusEnum;
import wcg.yhm.user.biz.mapper.PermissionMapper;
import wcg.yhm.user.biz.mapper.RoleMapper;
import wcg.yhm.user.biz.mapper.RolePermissionRelMapper;
import wcg.yhm.user.biz.service.RbacCacheService;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionRelServiceImplTest {

    @Mock private RolePermissionRelMapper relationMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private PermissionMapper permissionMapper;
    @Mock private RbacCacheService cacheService;

    private RolePermissionRelServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(
                new MybatisConfiguration(), RolePermissionRel.class.getName()), RolePermissionRel.class);
        service = spy(new RolePermissionRelServiceImpl());
        ReflectionTestUtils.setField(service, "baseMapper", relationMapper);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "permissionMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "rbacCacheService", cacheService);
    }

    @Test
    void assignPermissionsDeduplicatesInputAndRefreshesCache() {
        when(roleMapper.selectById(3L)).thenReturn(new Role().setId(3L).setRoleKey("editor")
                .setStatus(StatusEnum.ENABLE.getValue()).setIsDeleted(DeletedEnum.NO.getValue()));
        when(permissionMapper.selectCount(any())).thenReturn(2L);
        when(relationMapper.selectList(any())).thenReturn(List.of());
        doReturn(true).when(service).saveBatch(anyCollection());
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(cacheService).afterCommit(any(Runnable.class));

        service.assignPermissions(3L, List.of(10L, 10L, 11L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<RolePermissionRel>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(service).saveBatch(captor.capture());
        assertThat(captor.getValue()).extracting(RolePermissionRel::getPermissionId)
                .containsExactlyInAnyOrder(10L, 11L);
        verify(cacheService).refreshRolePermissions(3L);
    }

    @Test
    void emptyPermissionListRevokesEveryActiveRelation() {
        RolePermissionRel activeRelation = new RolePermissionRel().setId(30L).setRoleId(3L)
                .setPermissionId(10L).setIsDeleted(DeletedEnum.NO.getValue());
        when(roleMapper.selectById(3L)).thenReturn(new Role().setId(3L).setRoleKey("editor")
                .setStatus(StatusEnum.ENABLE.getValue()).setIsDeleted(DeletedEnum.NO.getValue()));
        when(relationMapper.selectList(any())).thenReturn(List.of(activeRelation));
        doReturn(true).when(service).updateBatchById(anyCollection());
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(cacheService).afterCommit(any(Runnable.class));

        service.assignPermissions(3L, List.of());

        assertThat(activeRelation.getIsDeleted()).isTrue();
        verify(service, never()).saveBatch(anyCollection());
        verify(cacheService).refreshRolePermissions(3L);
    }
}
