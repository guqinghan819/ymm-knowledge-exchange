package wcg.yhm.ai.biz.agent;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import wcg.yhm.ai.biz.domain.request.AiWriteContentRequest;

import java.util.List;

/**
 * 图文笔记正文润色 Agent
 */
@Component
public class NoteContentPolishAgent {

    @Resource
    private ChatModel chatModel;

    public String polish(List<String> knowledgeSummaries, AiWriteContentRequest request) {
        UserMessage userMessage = UserMessage.from(buildPrompt(knowledgeSummaries, request));
        ChatResponse response = chatModel.chat(userMessage);
        return response.aiMessage().text();
    }

    private String buildPrompt(List<String> knowledgeSummaries, AiWriteContentRequest request) {
        return """
                你是知识分享社区的中文图文笔记润色 Agent，也是一位认真观察生活、善于提炼经验的创作者。
                请基于图片解析出的内容与知识点，写一段适合发布的正文，让读者既能看懂图片表达，也能获得一点有价值的收获。

                写作要求：
                1. 符合知识分享目标：正文必须包含信息、观察、经验、方法、提醒或启发中的至少两类；
                2. 有感染力但不夸张：像真实创作者在认真分享，语气自然、真诚、有温度；
                3. 结构建议：开头用1句话点出图片主题或场景，中间提炼可学习/可借鉴的内容，结尾给出温和的感受或启发；
                4. 内容要具体：优先使用图片里能看到的主体、细节、动作、环境，不要写空泛套话；
                5. 不要编造图片中无法判断的人名、地点、品牌、时间、数据、功效和因果关系；
                6. 不要使用营销口吻，不要使用“震惊、必看、封神、绝绝子、家人们”等夸张表达；
                7. 可以自然分段，但不要使用 Markdown 标题、编号列表或项目符号；
                8. 字数控制在180到360字；
                9. 只输出正文内容，不要解释任务，不要额外输出标题。

                图片内容与知识点：
                %s

                当前标题：%s
                已有正文草稿：%s
                """.formatted(
                String.join("\n", knowledgeSummaries),
                StrUtil.blankToDefault(request.getTitle(), "暂无"),
                StrUtil.blankToDefault(request.getContent(), "暂无")
        );
    }
}
