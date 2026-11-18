package wcg.yhm.oss.biz.config.cos;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cos.video.multipart")
public class CosVideoMultipartProperties {
    private boolean enabled = true;
    private long thresholdBytes = 20L * 1024 * 1024;
    private long maxFileSizeBytes = 500L * 1024 * 1024;
    private long partSizeBytes = 8L * 1024 * 1024;
    private long signatureTtlSeconds = 15L * 60;
    private long taskTtlHours = 24;
    private long completeLockSeconds = 30;
}
