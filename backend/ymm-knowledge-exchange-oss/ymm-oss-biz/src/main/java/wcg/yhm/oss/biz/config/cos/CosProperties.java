package wcg.yhm.oss.biz.config.cos;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-27
 * @Description: cos存储配置类
 * @Version: 1.0
 */

@Component
@Data
@ConfigurationProperties(prefix = "cos")
public class CosProperties {
    private String host; //域名
    private String secretId; //id
    private String secretKey; //密钥
    private String bucket; //桶名
    private String regin; //区域
}
    
