package wcg.yhm.oss.biz.domain.model;

import lombok.Data;

@Data
public class CosVideoMultipartSession {
    public static final String ACTIVE = "ACTIVE";
    public static final String COMPLETED = "COMPLETED";

    private String taskId;
    private Long userId;
    private String uploadId;
    private String objectKey;
    private String originalFileName;
    private long fileSize;
    private String contentType;
    private Long lastModified;
    private long partSize;
    private int totalParts;
    private String status;
    private String resultUrl;
    private long expiresAt;
}
