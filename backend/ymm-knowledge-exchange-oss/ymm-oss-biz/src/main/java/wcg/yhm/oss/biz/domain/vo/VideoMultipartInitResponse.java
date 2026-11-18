package wcg.yhm.oss.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoMultipartInitResponse {
    private String taskId;
    private long partSize;
    private int totalParts;
    private long expiresAt;
}
