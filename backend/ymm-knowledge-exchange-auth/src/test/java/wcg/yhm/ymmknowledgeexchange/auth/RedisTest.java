package wcg.yhm.ymmknowledgeexchange.auth;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description:
 * @Version: 1.0
 */

@SpringBootTest
public class RedisTest {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void test1() {
        redisTemplate.opsForValue().set("name66", "1234");
    }
}
    
