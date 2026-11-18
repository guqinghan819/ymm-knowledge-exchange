package wcg.yhm.ai.biz.agent.result;

import lombok.Data;

import java.util.List;

/**
 * 识图写作润色结果
 */
@Data
public class ImageWritingResult {

    private String title;

    private String summary;

    private String content;

    private List<String> highlights;

    private List<String> tags;
}
