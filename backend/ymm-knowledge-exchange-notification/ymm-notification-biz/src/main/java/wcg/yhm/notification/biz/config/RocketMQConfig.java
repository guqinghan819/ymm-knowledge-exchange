package wcg.yhm.notification.biz.config;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-24
 * @Description: rocketmq配置类
 * @Version: 1.0
 */

@Configuration
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {
}
    
