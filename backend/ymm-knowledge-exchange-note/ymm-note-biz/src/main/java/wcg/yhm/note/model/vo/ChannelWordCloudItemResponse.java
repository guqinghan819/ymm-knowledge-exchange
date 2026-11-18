package wcg.yhm.note.model.vo;

import lombok.Data;

@Data
public class ChannelWordCloudItemResponse {

    private String word;

    private Long count = 0L;

    private Long weight = 0L;
}
