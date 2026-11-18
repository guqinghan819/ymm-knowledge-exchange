package wcg.yhm.note.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 雨好绵
 * @Description: 管理端话题列表响应(含所属频道)
 */
@Data
public class TopicAdminResponse {

    private Long id;

    private String name;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 该话题所属的频道列表
     */
    private List<ChannelResponse> channels;
}
