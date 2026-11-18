package wcg.yhm.user.biz.constant;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: redis的key常量类
 * @Version: 1.0
 */

public interface RedisKeyConstant {

    //redis全局唯一id前缀
    String YMM_ID_GENERATOR_KEY = "ymm_id_generator_key";
    //用户角色key前缀
    String USER_ROLES_KEY_PREFIX = "user:roles:";
    //角色权限key前缀
    String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
    //用户信息缓存key前缀
    String USER_INFO_KEY_PREFIX = "user:info:";
}
    
