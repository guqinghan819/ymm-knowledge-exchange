package wcg.yhm.ymmknowledgeexchange.auth.api.sms;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description:
 * @Version: 1.0
 */

@Configuration
@Slf4j
public class AliyunSmsClientConfig {

    @Resource
    private AliyunAccessKey aliyunAccessKey;

    @Bean
    public com.aliyun.dypnsapi20170525.Client client() throws Exception {
        // 工程代码建议使用更安全的无AK方式，凭据配置方式请参见：https://help.aliyun.com/document_detail/378657.html。
        com.aliyun.credentials.Client credential = new com.aliyun.credentials.Client();
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setCredential(credential);
        // Endpoint 请参考 https://api.aliyun.com/product/Dypnsapi
        config.endpoint = "dypnsapi.aliyuncs.com";
        config.setAccessKeyId(aliyunAccessKey.getAccessKeyId());
        config.setAccessKeySecret(aliyunAccessKey.getAccessKeySecret());
        return new com.aliyun.dypnsapi20170525.Client(config);
    }
}
    
