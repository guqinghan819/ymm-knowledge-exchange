package wcg.yhm.framework.context.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import wcg.yhm.framework.context.filter.SaveUserContextFilter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-25
 * @Description:
 * @Version: 1.0
 */

@AutoConfiguration
public class ContextUtilConfig {

    @Bean
    public SaveUserContextFilter saveUserContextFilter() {
        return new SaveUserContextFilter();
    }
}
    
