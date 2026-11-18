package wcg.yhm.ai.biz.agent.pptx.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import wcg.yhm.ai.biz.agent.result.TemplateSelectionResult;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-15
 * @Description: 模板选择助手
 * @Version: 1.0
 */

public interface TemplateAssistant {

    @SystemMessage("""
            ## 角色
            你是PPT模板选择专家。

            ## 任务
            根据PPT需求，从可用模板中选择最合适的一个。

            ## 输出要求
            输出JSON格式：
            {
              "templateCode": "选择的模板编码",
              "reason": "选择原因"
            }

            选择标准：
            1. 风格匹配：根据需求中的风格要求（商务、科技、简约等）选择匹配的模板
            2. 页数匹配：根据需求中的页数要求选择合适的模板
            3. 场景匹配：根据需求描述的使用场景选择合适的模板

            注意：必须从可用模板中选择一个，不能自定义。
            """)
    @UserMessage("""
            PPT需求：{{requirement}}
            可用模板：{{templateInfo}}
            """)
    TemplateSelectionResult select(@V("requirement") String requirement, @V("templateInfo") String templateInfo);
}
