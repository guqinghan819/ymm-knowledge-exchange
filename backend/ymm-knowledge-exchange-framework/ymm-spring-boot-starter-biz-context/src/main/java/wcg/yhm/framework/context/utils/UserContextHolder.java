package wcg.yhm.framework.context.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import wcg.yhm.ymmknowledgeexchange.common.constant.GlobalConstant;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-25
 * @Description: 在当前请求及异步任务中传递用户 ID，使用后须清理线程上下文
 * @Version: 1.0
 */

public class UserContextHolder {

    private static final ThreadLocal<Map<String, Object>> USER_CONTEXT_MAP =
            TransmittableThreadLocal.withInitial(HashMap::new); //直接直接初始化一个map，防止后续没有set一个map获取值报错
//    private static final ThreadLocal<Map<String, Object>> USER_CONTEXT_MAP =
//            ThreadLocal.withInitial(HashMap::new);

    /**
     * 保存用户id
     * @param value 用户id
     */
    public static void setUserId(Object value) {
        Map<String, Object> map = USER_CONTEXT_MAP.get();
        map.put(GlobalConstant.USER_ID, value);
    }

    /**
     * 获取用户id
     * @return 用户id
     */
    public static Long getUserId() {
        Object userId = USER_CONTEXT_MAP.get().get(GlobalConstant.USER_ID);
        if (userId == null) {
            return null;
        }
        return Long.valueOf(userId.toString());
    }

    /**
     * 删除上下文信息
     */
    public static void removeThreadLocal() {
        // 线程池会复用线程，必须移除旧用户信息以免泄漏到下一个请求。
        USER_CONTEXT_MAP.remove();
    }
}
    
