package wcg.yhm.ymmknowledgeexchange.gateway.constant;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-24
 * @Description: key常量类
 * @Version: 1.0
 */

public interface RedisKeyConstant {
    /**
     * 用户对应角色集合 KEY 前缀
     */
    String USER_ROLES_KEY_PREFIX = "user:roles:";

    /**
     * 角色对应的权限集合 KEY 前缀
     */
    String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
}
    
