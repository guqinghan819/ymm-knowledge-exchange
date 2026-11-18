package wcg.yhm.user.relation.biz.config;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description:
 * @Version: 1.0
 */

@Configuration
@RefreshScope
@Slf4j
public class FollowUnFollowRateLimitConfig {
    @Value("${mq-consumer.follow-unfollow.rate-limit}")
    private double rateLimit;

    @Bean
    @RefreshScope
    public RateLimiter rateLimiter() {
        log.info("======> rateLimit: {}", rateLimit);
        return RateLimiter.create(rateLimit);
    }
}
    
