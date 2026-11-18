package wcg.yhm.oss.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoMultipartSignResponse {
    private String uploadUrl;
    private long expiresAt;
}
