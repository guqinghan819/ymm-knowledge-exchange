package wcg.yhm.note.model.vo;

import lombok.Data;

/**
 * 管理端频道词云项。
 */
@Data
public class AdminChannelWordCloudResponse {

    private String name;

    private Long count = 0L;
}
