package wcg.yhm.oss.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VideoMultipartStatusResponse {
    private String status;
    private List<VideoMultipartPartResponse> uploadedParts;
    private long uploadedBytes;
    private long fileSize;
    private long expiresAt;
    private String url;
}
