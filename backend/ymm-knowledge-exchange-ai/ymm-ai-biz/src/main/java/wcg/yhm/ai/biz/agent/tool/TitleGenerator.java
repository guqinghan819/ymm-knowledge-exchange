package wcg.yhm.ai.biz.agent.tool;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-09
 * @Description: 对话标题生成器
 * @Version: 1.0
 */

@Component
@Slf4j
public class TitleGenerator {

    @Resource
    private ChatModel chatModel;

    /**
     * 根据用户首个问题生成简洁的对话标题（不超过15字）
     */
    public String generate(String firstQuery) {
        SystemMessage systemMessage = SystemMessage.from("""
                ## 角色
                你是一个标题精简助手。根据用户输入的第一个问题，生成一个简短的对话标题。

                ## 规则
                1. 标题长度不超过15个字
                2. 保留核心主题，去掉语气词、客套话
                3. 不要包含「请问」「帮我」「如何」「怎么」等开头引导词，直接提取主题
                4. 只返回标题文本，不要加引号、标点、解释

                ## 示例
                输入：今天有什么关于人工智能的最新新闻吗
                输出：AI最新新闻

                输入：帮我对比一下Java 21和Java 17的新特性
                输出：Java 21与17对比

                输入：为什么天空是蓝色的能不能用简单的语言解释一下
                输出：天空为什么是蓝色
                """);

        UserMessage userMessage = UserMessage.from(firstQuery);

        try {
            ChatResponse response = chatModel.chat(systemMessage, userMessage);
            String title = response.aiMessage().text();
            if (StrUtil.isNotBlank(title)) {
                return title.trim();
            }
        } catch (Exception e) {
            log.warn("生成标题失败, query={}", firstQuery, e);
        }
        return firstQuery.length() > 15 ? firstQuery.substring(0, 15) : firstQuery;
    }
}
