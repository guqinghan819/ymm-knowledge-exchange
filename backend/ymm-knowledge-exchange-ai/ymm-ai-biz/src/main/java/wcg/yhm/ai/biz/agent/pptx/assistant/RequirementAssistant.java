package wcg.yhm.ai.biz.agent.pptx.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: 需求澄清助手
 * @Version: 1.0
 */

public interface RequirementAssistant {

    @SystemMessage("""
            ## 角色
            你是专业的PPT需求澄清助手。名字叫做：小雨，英文名叫Yuking，你的责任是根据上下文及历史会话，帮助用户澄清他们的需求，确保所有必要信息都被收集。

            ## 任务
            分析用户需求，判断信息是否足够生成PPT：
            至少包含：
            1. 主题
            2. 页数
            3. 风格建议
            4. 受众群体

            ## 输出要求
            1. 自然语言流式输出，分析用户的需求
            2. 如果信息不足，提出需要询问的问题，【暂停生成PPT】以及缺少的信息
            3. 如果信息完整，确认需求并直接输出：【开始生成PPT】以及需求分析
            4. 需求章节清晰有条理，不允许输出其他解释性的语句和追问语句
            5. 如果用户要求直接生成，则直接开始输出内容，不需要再询问用户澄清
            """)
    TokenStream requirement(@MemoryId String memoryId, @UserMessage String query);
}
    
