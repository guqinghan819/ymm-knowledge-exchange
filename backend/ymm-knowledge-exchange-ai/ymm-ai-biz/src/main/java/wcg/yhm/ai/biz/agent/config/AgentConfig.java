package wcg.yhm.ai.biz.agent.config;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wcg.yhm.ai.biz.agent.pptx.assistant.*;
import wcg.yhm.ai.biz.agent.WebSearchAgent;
import wcg.yhm.ai.biz.agent.tool.TavilySearchTool;

/**
 * AI 服务配置类
 * 
 * 配置带有 Tavily 网络搜索功能的 AI Service
 */
@Configuration
public class AgentConfig {

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private TavilySearchTool tavilySearchTool;

    @Resource
    private ChatMemoryProvider chatMemoryProvider;

    @Resource
    private ChatModel chatModel;

    @Resource(name = "intentChatModel")
    private ChatModel intentChatModel;

    @Resource(name = "templateChatModel")
    private ChatModel templateChatModel;

    /**
     * 创建带有网络搜索功能的 WebSearchAiService
     * 
     * 关键点：
     * 1. 通过 .tools() 方法注册 TavilySearchTool
     * 2. AI 会根据问题自动判断是否需要调用搜索工具
     * 3. 支持流式输出和多轮对话记忆
     */
    @Bean
    public WebSearchAgent webSearchAgent() {
        return AgenticServices.agentBuilder(WebSearchAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tavilySearchTool)
                .build();
    }

    /**
     * 意识识别
     */
    @Bean
    public IntentRecognitionAssistant intentRecognitionAssistant() {
        return AiServices.builder(IntentRecognitionAssistant.class)
                .chatModel(intentChatModel)
                .build();

    }

    /**
     *需求澄清
     */
    @Bean
    public RequirementAssistant requirementAssistant() {
        return AiServices.builder(RequirementAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    /**
     * 信息收集
     */
    @Bean
    public SearchAssistant searchAssistant() {
        return AiServices.builder(SearchAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tavilySearchTool)
                .build();
    }

    /**
     *大纲生成
     */
    @Bean
    public OutLineAssistant outLineAssistant() {
        return AiServices.builder(OutLineAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    /**
     * 模板选择
     */
    @Bean
    public TemplateAssistant templateAssistant() {
        return AiServices.builder(TemplateAssistant.class)
                .chatModel(templateChatModel)
                .build();
    }

    /**
     * Schema生成
     */
    @Bean
    public SchemaAssistant schemaAssistant() {
        return AiServices.builder(SchemaAssistant.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    /**
     * 错误提示
     */
    public ErrorAssistant errorAssistant() {
        return AiServices.builder(ErrorAssistant.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }

}
