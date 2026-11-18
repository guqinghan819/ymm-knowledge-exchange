package wcg.yhm.note.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 雨好绵
 * @Description: 管理端频道列表响应(含绑定话题)
 */
@Data
public class ChannelAdminResponse {

    private Long id;

    private String name;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 该频道下绑定的全部话题
     */
    private List<TopicResponse> topics;
}
