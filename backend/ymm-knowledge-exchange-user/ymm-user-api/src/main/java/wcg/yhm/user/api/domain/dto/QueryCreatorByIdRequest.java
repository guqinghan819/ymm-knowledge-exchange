package wcg.yhm.user.api.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-24
 * @Description: 根据id查询创作者信息请求
 * @Version: 1.0
 */

@Data
public class QueryCreatorByIdRequest implements Serializable {
    /**
     * 创作者id
     */
    @NotNull(message = "创作者id不能为空")
    private Long creatorId;
}
    
