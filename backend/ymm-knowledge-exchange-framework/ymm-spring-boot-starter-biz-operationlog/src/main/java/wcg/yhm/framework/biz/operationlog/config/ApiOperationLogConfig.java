package wcg.yhm.framework.biz.operationlog.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLogAspect;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-20
 * @Description: api操作日志配置类
 * @Version: 1.0
 */

@AutoConfiguration
public class ApiOperationLogConfig {

    @Bean
    public ApiOperationLogAspect apiOperationLogAspect() {
        return new ApiOperationLogAspect();
    }
}
    
