package wcg.yhm.ai.biz.agent.result;

import lombok.Data;

import java.util.List;

/**
 * 发布助手自动配图提示词
 */
@Data
public class PublishImagePromptResult {

    private List<String> prompts;
}
