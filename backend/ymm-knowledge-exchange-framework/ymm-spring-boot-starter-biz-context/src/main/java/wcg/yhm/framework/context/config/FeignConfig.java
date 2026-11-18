package wcg.yhm.framework.context.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import wcg.yhm.framework.context.interceptor.FeignInterceptor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-28
 * @Description: feign拦截器自动配置
 * @Version: 1.0
 */

@AutoConfiguration
public class FeignConfig {

    @Bean
    public FeignInterceptor feignInterceptor() {
        return new FeignInterceptor();
    }
}
    
