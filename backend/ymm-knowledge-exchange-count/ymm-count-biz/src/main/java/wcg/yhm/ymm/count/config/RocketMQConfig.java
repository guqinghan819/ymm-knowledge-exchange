package wcg.yhm.ymm.count.config;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-03
 * @Description:
 * @Version: 1.0
 */

@Configuration
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {
}
    
