package wcg.yhm.ai.biz.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * AI 发布助手请求
 */
@Data
public class PublishAssistantRequest {

    @NotBlank(message = "发布需求不能为空")
    private String query;

    @NotBlank(message = "会话id不能为空")
    private String conversationId;

    private List<String> imageUrls;

    /**
     * 用户在发布助手输入区提前选择的频道/话题，未选择时仍由模型从 query 中识别。
     */
    private Long channelId;

    private String channelName;

    private Long topicId;

    private String topicName;
}
