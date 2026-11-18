package wcg.yhm.ymm.data.align.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-16
 * @Description:
 * @Version: 1.0
 */

@Component
@ConfigurationProperties(prefix = XxlJobProperties.PREFIX)
@Data
public class XxlJobProperties {
    public static final String PREFIX = "xxl.job";

    private String adminAddresses;

    private String accessToken;

    private String appName;

    private String ip;

    private int port;

    private String logPath;

    private int logRetentionDays = 30;
}
    
