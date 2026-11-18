package wcg.yhm.ai.biz.domain.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * AI 写图文笔记标题请求
 */
@Data
public class AiWriteTitleRequest {

    @NotEmpty(message = "图片地址列表不能为空")
    private List<String> imageUrls;

    private String title;

    private String content;

    private String conversationId;
}
