package wcg.yhm.ai.biz.agent.result;

import lombok.Data;

import java.util.List;

/**
 * 发布助手意图抽取结果
 */
@Data
public class PublishAssistantIntent {

    private Boolean publishIntent;

    private String channelName;

    private String topicName;

    private String title;

    private String contentIdea;

    private Integer contentWordCount;

    private List<String> tags;
}
