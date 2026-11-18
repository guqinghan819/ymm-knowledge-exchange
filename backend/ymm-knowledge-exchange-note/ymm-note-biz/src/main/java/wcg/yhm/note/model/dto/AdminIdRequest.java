package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: 雨好绵
 * @Description: 管理端按id操作请求(删除频道/话题)
 */
@Data
public class AdminIdRequest {

    @NotNull(message = "id不能为空")
    private Long id;
}
