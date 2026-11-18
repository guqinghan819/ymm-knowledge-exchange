package wcg.yhm.ymm.search.biz.canal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description: canal配置选项类
 * @Version: 1.0
 */

@ConfigurationProperties(prefix = "canal")
@Component
@Data
public class CanalProperties {

    private String address;

    private String destination;

    private String username;

    private String password;

    private String subscribe;

    private int batchSize;
}
    
