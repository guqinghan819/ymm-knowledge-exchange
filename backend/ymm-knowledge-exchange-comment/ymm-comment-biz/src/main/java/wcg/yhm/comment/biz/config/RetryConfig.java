package wcg.yhm.comment.biz.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import wcg.yhm.comment.biz.config.properties.RetryProperties;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description: 重试策略配置类
 * @Version: 1.0
 */


@Configuration
public class RetryConfig {
    @Resource
    private RetryProperties retryProperties;


    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        //定制重试次数
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(retryProperties.getMaxAttempts());

        //定制重试间隔
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(retryProperties.getInitInterval());
        exponentialBackOffPolicy.setMultiplier(retryProperties.getMultiplier());

        retryTemplate.setRetryPolicy(simpleRetryPolicy);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);

        return retryTemplate;
    }
}
    
