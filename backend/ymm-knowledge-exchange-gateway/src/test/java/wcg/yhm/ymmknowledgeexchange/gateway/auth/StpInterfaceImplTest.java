package wcg.yhm.ymmknowledgeexchange.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.ymmknowledgeexchange.gateway.constant.RedisKeyConstant;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StpInterfaceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private StpInterfaceImpl stpInterface;

    @BeforeEach
    void setUp() {
        stpInterface = new StpInterfaceImpl();
        ReflectionTestUtils.setField(stpInterface, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(stpInterface, "objectMapper", new ObjectMapper());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void returnsEmptyCollectionsWhenRoleCacheIsMissing() {
        when(valueOperations.get(RedisKeyConstant.USER_ROLES_KEY_PREFIX + "42")).thenReturn(null);

        assertThat(stpInterface.getRoleList(42L, "login")).isEmpty();
        assertThat(stpInterface.getPermissionList(42L, "login")).isEmpty();
    }

    @Test
    void mergesAndDeduplicatesPermissionsFromMultipleRoles() {
        when(valueOperations.get(RedisKeyConstant.USER_ROLES_KEY_PREFIX + "42"))
                .thenReturn("[\"common_user\",\"editor\",\"editor\"]");
        when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "[\"app:note:publish\",\"app:comment:publish\"]",
                "[\"app:note:publish\",\"app:note:edit\"]"));

        assertThat(stpInterface.getRoleList(42L, "login"))
                .containsExactly("common_user", "editor");
        assertThat(stpInterface.getPermissionList(42L, "login"))
                .containsExactly("app:note:publish", "app:comment:publish", "app:note:edit");
    }

    @Test
    void ignoresMalformedPermissionEntryWithoutGrantingItsPermissions() {
        when(valueOperations.get(RedisKeyConstant.USER_ROLES_KEY_PREFIX + "42"))
                .thenReturn("[\"common_user\",\"broken\"]");
        when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "[\"app:note:publish\"]", "not-json"));

        assertThat(stpInterface.getPermissionList(42L, "login"))
                .containsExactly("app:note:publish");
    }

    @Test
    void malformedRoleEntryFailsClosed() {
        when(valueOperations.get(RedisKeyConstant.USER_ROLES_KEY_PREFIX + "42"))
                .thenReturn("not-json");

        assertThat(stpInterface.getRoleList(42L, "login")).isEmpty();
        assertThat(stpInterface.getPermissionList(42L, "login")).isEmpty();
    }
}
