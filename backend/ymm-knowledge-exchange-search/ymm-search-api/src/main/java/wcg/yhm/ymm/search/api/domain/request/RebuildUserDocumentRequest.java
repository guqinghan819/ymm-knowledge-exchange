package wcg.yhm.ymm.search.api.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description: 重构用户文档请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RebuildUserDocumentRequest {
    @NotNull(message = "用户id不能为空")
    private Long userId;
}
    
