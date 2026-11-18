package wcg.yhm.user.biz.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import wcg.yhm.user.api.domain.vo.CreatorResponse;

import java.util.concurrent.TimeUnit;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-22
 * @Description:本地缓存工具类
 * @Version: 1.0
 */

public class CaffeineUtil {
    private static final Cache<Long, CreatorResponse> USER_LOCAL_CACHE =
            Caffeine.newBuilder()
                    .initialCapacity(10000) //设置初始条目为10000
                    .maximumSize(10000) //设置缓存最大容量为10000
                    .expireAfterWrite(1, TimeUnit.HOURS) //设置过期时间为1小时
                    .build();

    /**
     * 根据用户id查询用户信息
     * @param userId 用户id
     * @return 用户信息响应类
     */
    public static CreatorResponse getUserInfo(Long userId) {
        return USER_LOCAL_CACHE.getIfPresent(userId);
    }

    /**
     * 保存用户信息
     * @param creatorResponse 用户信息
     */
    public static void saveUserInfo(Long userId, CreatorResponse creatorResponse) {
        USER_LOCAL_CACHE.put(userId, creatorResponse);
    }
}
    
