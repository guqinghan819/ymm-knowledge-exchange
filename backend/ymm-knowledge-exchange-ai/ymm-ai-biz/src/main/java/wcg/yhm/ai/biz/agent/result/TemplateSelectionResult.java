package wcg.yhm.ai.biz.agent.result;

import dev.langchain4j.model.output.structured.Description;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-15
 * @Description: 模板选择结果
 * @Version: 1.0
 */

public record TemplateSelectionResult(
        @Description("选择的模板编码")
        String templateCode,
        @Description("选择原因")
        String reason) {}
