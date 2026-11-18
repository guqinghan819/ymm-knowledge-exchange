package wcg.yhm.oss.biz.config.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: Minio配置属性类
 * @Version: 1.0
 */

@ConfigurationProperties(prefix = "minio")
@Component
@Data
public class MinioProperties {

    private String endPoint; // 服务地址
    private String accessKey; // 用户名
    private String SecretKey; // 密码
    private String Bucket; //桶名
}
    
