package wcg.yhm.ai.biz.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-07
 * @Description: 问题推荐工具
 * @Version: 1.0
 */

@Component
@Slf4j
public class RecommendTool {

    @Resource
    private ChatModel chatModel;

    @Tool("根据当前用户问题和联网搜索工具调用结果生成相关推荐问题")
    public List<String> recommend(@P("用户当前问题") String currentQuestion, @P("联网搜索结果") String toolResult) {
        SystemMessage systemMessage = SystemMessage.from("""
                ## 角色
                你是一名问题推荐智能助手，专门根据用户的当前问题与工具调用结果(有的话)生成相关的问题
                
                ## 要求
                1.至少生成5个相关的问题
                2.生成的问题必须与用户当前问题和联网搜索结果相挂钩
                3.只返回一个JSON数组格式的字符串，不要有其他解释文字
                4.示例格式：["问题1", "问题2", "问题3"]
                """);

        UserMessage userMessage = UserMessage.from(String.format("""
                用户当前问题：%s
                
                联网搜索结果：%s
                
                请根据以上信息生成5个相关问题，只返回JSON数组格式。
                """, currentQuestion, toolResult != null ? toolResult : "无"));

        ChatResponse response = chatModel.chat(systemMessage, userMessage);
        String result = response.aiMessage().text();

        try {
            // 解析 JSON 数组
            return wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil.str2List(result, String.class);
        } catch (Exception e) {
            log.warn("解析推荐问题失败，返回默认问题", e);
            return List.of(
                    "还有什么想了解的吗？",
                    "需要我帮你深入分析吗？",
                    "还有其他问题需要解答吗？"
            );
        }
    }
}
    
