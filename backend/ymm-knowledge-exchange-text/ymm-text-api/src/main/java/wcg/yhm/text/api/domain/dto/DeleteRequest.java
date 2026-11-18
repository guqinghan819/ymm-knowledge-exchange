package wcg.yhm.text.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-04
 * @Description: 删除请求
 * @Version: 1.0
 */

@Data
public class DeleteRequest implements Serializable {

    @NotBlank(message = "id不能为空")
    private String id;
}
    
