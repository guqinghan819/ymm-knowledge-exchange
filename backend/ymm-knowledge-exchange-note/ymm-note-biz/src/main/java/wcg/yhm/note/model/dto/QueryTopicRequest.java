package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-16
 * @Description:
 * @Version: 1.0
 */

@Data
public class QueryTopicRequest {
    @NotNull(message = "channelId不能为空")
    private Long id;
}
    
