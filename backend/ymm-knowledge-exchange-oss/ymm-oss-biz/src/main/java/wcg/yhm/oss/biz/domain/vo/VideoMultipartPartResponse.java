package wcg.yhm.oss.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VideoMultipartPartResponse {
    private int partNumber;
    private long size;
}
