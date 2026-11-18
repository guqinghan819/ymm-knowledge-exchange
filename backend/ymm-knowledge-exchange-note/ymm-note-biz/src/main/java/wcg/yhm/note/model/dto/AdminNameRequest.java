package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: 雨好绵
 * @Description: 管理端新增名称请求(新增频道/话题)
 */
@Data
public class AdminNameRequest {

    @NotBlank(message = "名称不能为空")
    private String name;
}
