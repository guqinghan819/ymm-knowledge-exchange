package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @Author: 雨好绵
 * @Description: 管理端绑定频道话题请求
 */
@Data
public class BindTopicsRequest {

    @NotNull(message = "频道id不能为空")
    private Long channelId;

    /**
     * 话题id列表(为空表示清空该频道下所有话题绑定)
     */
    private List<Long> topicIds;
}
