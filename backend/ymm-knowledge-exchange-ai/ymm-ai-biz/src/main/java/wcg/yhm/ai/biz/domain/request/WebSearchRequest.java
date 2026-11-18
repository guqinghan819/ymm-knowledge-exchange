package wcg.yhm.ai.biz.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-10
 * @Description: 联网搜索请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebSearchRequest {

    @NotBlank(message = "问题不能为空")
    private String query;

    @NotBlank(message = "会话id不能为空")
    private String conversationId;
}
    
