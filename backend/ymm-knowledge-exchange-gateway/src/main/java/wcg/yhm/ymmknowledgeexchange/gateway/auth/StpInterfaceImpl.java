package wcg.yhm.ymmknowledgeexchange.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import wcg.yhm.ymmknowledgeexchange.gateway.constant.RedisKeyConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 自定义权限验证接口扩展
 * 官网建议的数据获取有三种，这里采用直接从redis中获取数据，但必须保证数据一致
 */
@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        String loginUserId = loginId.toString();
        List<String> roleList = getRoleList(loginId, loginType);
        if (roleList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> roleKeys = roleList.stream()
                .map(role -> RedisKeyConstant.ROLE_PERMISSIONS_KEY_PREFIX + role)
                .toList();
        List<Object> cachedPermissions = redisTemplate.opsForValue().multiGet(roleKeys);
        if (cachedPermissions == null || cachedPermissions.isEmpty()) {
            log.warn("No permission cache found for login user [{}]", loginUserId);
            return Collections.emptyList();
        }

        Set<String> permissions = new LinkedHashSet<>();
        for (Object cachedPermission : cachedPermissions) {
            if (!(cachedPermission instanceof String permissionJson) || permissionJson.isBlank()) {
                continue;
            }
            try {
                List<String> oneRolePermissions = objectMapper.readValue(
                        permissionJson, new TypeReference<List<String>>() { });
                if (oneRolePermissions != null) {
                    oneRolePermissions.stream()
                            .filter(permission -> permission != null && !permission.isBlank())
                            .forEach(permissions::add);
                }
            } catch (JsonProcessingException e) {
                // A malformed cache entry must never grant access. Other valid role entries can still be used.
                log.error("Failed to parse permission cache for login user [{}]", loginUserId, e);
            }
        }
        return new ArrayList<>(permissions);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        String loginUserId = loginId.toString();
        Object cachedRoles = redisTemplate.opsForValue()
                .get(RedisKeyConstant.USER_ROLES_KEY_PREFIX + loginUserId);
        if (!(cachedRoles instanceof String rolesJson) || rolesJson.isBlank()) {
            log.warn("No role cache found for login user [{}]", loginUserId);
            return Collections.emptyList();
        }
        try {
            List<String> roles = objectMapper.readValue(rolesJson, new TypeReference<List<String>>() { });
            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }
            return roles.stream()
                    .filter(role -> role != null && !role.isBlank())
                    .distinct()
                    .toList();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse role cache for login user [{}]", loginUserId, e);
            return Collections.emptyList();
        }
    }
}
