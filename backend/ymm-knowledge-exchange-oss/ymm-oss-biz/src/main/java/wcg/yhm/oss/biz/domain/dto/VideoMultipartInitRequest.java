package wcg.yhm.oss.biz.domain.dto;

import lombok.Data;

@Data
public class VideoMultipartInitRequest {
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Long lastModified;
}
