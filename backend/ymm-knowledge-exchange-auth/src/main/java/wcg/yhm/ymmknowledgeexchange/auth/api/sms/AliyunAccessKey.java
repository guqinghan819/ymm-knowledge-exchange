package wcg.yhm.ymmknowledgeexchange.auth.api.sms;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: aliyun accesskey
 * @Version: 1.0
 */

@Data
@Component
public class AliyunAccessKey {
    @Value("${aliyun.accessKeyId}")
    private String accessKeyId;
    @Value("${aliyun.accessKeySecret}")
    private String accessKeySecret;
}
    
