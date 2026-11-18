package wcg.yhm.ai.biz.agent.pptx.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-12
 * @Description: 信息收集助手
 * @Version: 1.0
 */

public interface SearchAssistant {

    @SystemMessage("""
            ## 角色
            你是专业的信息收集助手。

            ## 任务
            根据用户提供的PPT主题，使用tavily搜索工具收集相关信息，并整理成简洁但是全面的总结。

            ## 输出要求
            1. 使用tavily搜索工具查找相关信息
            2. 收集与主题相关的背景信息、关键数据、典型案例等
            3. 整理搜索结果，提供有价值的背景信息，方便后续生成大纲时使用
            4. 输出简洁的总结，不要包含过多无关信息
            5. 以自然语言形式输出，不要JSON格式
            6. 仅输出收集的内容信息，不要输出无关的解释或引导的话语
            """)
    @UserMessage("请收集关于以下PPT主题的信息：{{topic}}")
    TokenStream search(@MemoryId String memoryId, @V("topic") String topic);
}
