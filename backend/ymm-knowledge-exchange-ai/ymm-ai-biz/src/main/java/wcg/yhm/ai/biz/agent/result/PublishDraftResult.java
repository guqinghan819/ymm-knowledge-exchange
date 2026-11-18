package wcg.yhm.ai.biz.agent.result;

import lombok.Data;

import java.util.List;

/**
 * 发布助手预发布草稿
 */
@Data
public class PublishDraftResult {

    private String title;

    private String content;

    private String summary;

    private List<String> tags;
}
