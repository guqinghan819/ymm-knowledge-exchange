package wcg.yhm.text.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: 查询请求
 * @Version: 1.0
 */

@Data
public class QueryByIdRequest implements Serializable {

    @NotBlank(message = "id不能为空哦")
    private String id;
}
    
