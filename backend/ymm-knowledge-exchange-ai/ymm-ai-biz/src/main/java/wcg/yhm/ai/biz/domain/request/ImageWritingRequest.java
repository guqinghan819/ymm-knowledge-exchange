package wcg.yhm.ai.biz.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * AI 对话页识图写作请求
 */
@Data
public class ImageWritingRequest {

    @NotBlank(message = "写作要求不能为空")
    private String query;

    @NotBlank(message = "会话id不能为空")
    private String conversationId;

    @NotEmpty(message = "图片地址列表不能为空")
    private List<String> imageUrls;
}
