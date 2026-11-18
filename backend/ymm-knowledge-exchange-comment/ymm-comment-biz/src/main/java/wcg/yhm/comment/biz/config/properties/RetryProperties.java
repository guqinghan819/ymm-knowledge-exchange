package wcg.yhm.comment.biz.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description: 重试策略配置参数
 * @Version: 1.0
 */

@ConfigurationProperties(prefix = "retry")
@Component
@Data
public class RetryProperties {

    private Integer maxAttempts = 3; //最大重试次数

    private Integer initInterval = 1000; //初始延迟时间, ms

    private Double multiplier = 2.0; //重试间隔次数
}
    
