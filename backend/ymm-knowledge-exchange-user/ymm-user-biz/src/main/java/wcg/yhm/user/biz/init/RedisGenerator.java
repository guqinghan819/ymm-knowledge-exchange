package wcg.yhm.user.biz.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import wcg.yhm.user.biz.constant.RedisKeyConstant;


/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 初始化全局唯一id
 * @Version: 1.0
 */

@Component
@Slf4j
public class RedisGenerator {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化全局平台id
     */
    @PostConstruct
    public void init() {
        redisTemplate.opsForValue().setIfAbsent(RedisKeyConstant.YMM_ID_GENERATOR_KEY, 100000);
    }
}
    
