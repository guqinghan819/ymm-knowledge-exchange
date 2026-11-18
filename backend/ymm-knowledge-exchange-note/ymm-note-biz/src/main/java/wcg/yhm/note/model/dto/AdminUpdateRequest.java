package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: 雨好绵
 * @Description: 管理端修改名称请求(修改频道/话题)
 */
@Data
public class AdminUpdateRequest {

    @NotNull(message = "id不能为空")
    private Long id;

    @NotBlank(message = "名称不能为空")
    private String name;
}
