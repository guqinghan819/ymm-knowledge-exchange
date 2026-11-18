package wcg.yhm.ai.biz.agent.result;

import dev.langchain4j.model.output.structured.Description;
import wcg.yhm.ai.biz.agent.enums.pptx.PptIntent;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-10
 * @Description: 意图结果
 * @Version: 1.0
 */

public record PptIntentResult(
        @Description("意图枚举类")
        PptIntent pptIntent,
        @Description("选择原因")
        String reason) {}
    
