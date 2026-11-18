package wcg.yhm.ymmknowledgeexchange.auth.constant;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: redis的key常量类
 * @Version: 1.0
 */

public interface RedisKeyConstant {
    //登陆验证码
    String VERIFICATION_CODE_LOGIN_KEY_PREFIX = "verification_login_code:";
    //更新密码验证码
    String VERIFICATION_CODE_UPDATE_KEY_PREFIX = "verification_update_code:";
    //redis全局唯一id前缀
    String YMM_ID_GENERATOR_KEY = "ymm_id_generator_key";
    //用户角色key前缀
    String USER_ROLES_KEY_PREFIX = "user:roles:";
    //角色权限key前缀
    String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";
}
    
