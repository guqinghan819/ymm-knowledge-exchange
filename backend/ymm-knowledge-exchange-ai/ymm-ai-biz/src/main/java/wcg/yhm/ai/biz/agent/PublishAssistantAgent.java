package wcg.yhm.ai.biz.agent;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.ai.biz.agent.result.PublishAssistantIntent;
import wcg.yhm.ai.biz.agent.result.PublishDraftResult;
import wcg.yhm.ai.biz.agent.result.PublishImagePromptResult;
import wcg.yhm.note.api.domain.response.CatalogItemResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.Collections;
import java.util.List;

/**
 * AI 发布助手主干 Agent。
 */
@Component
@Slf4j
public class PublishAssistantAgent {

    @Resource
    private ChatModel chatModel;

    public PublishAssistantIntent extractIntent(String query, List<CatalogItemResponse> channels) {
        String rawText = chat(buildExtractPrompt(query, channels));
        try {
            PublishAssistantIntent intent = parseObject(rawText, PublishAssistantIntent.class);
            normalizeIntent(intent);
            return intent;
        } catch (Exception e) {
            log.warn("发布助手意图抽取 JSON 不合规，尝试修复: {}", rawText, e);
            PublishAssistantIntent intent = parseObject(repairJson(rawText, "PublishAssistantIntent"), PublishAssistantIntent.class);
            normalizeIntent(intent);
            return intent;
        }
    }

    public List<String> buildImagePrompts(String query, PublishAssistantIntent intent) {
        String rawText = chat(buildImagePrompt(query, intent));
        try {
            PublishImagePromptResult result = parseObject(rawText, PublishImagePromptResult.class);
            return normalizeList(result.getPrompts(), 3, 180);
        } catch (Exception e) {
            log.warn("发布助手配图提示词 JSON 不合规，尝试修复: {}", rawText, e);
            PublishImagePromptResult result = parseObject(repairJson(rawText, "PublishImagePromptResult"), PublishImagePromptResult.class);
            return normalizeList(result.getPrompts(), 3, 180);
        }
    }

    public PublishDraftResult buildDraft(String query,
                                         PublishAssistantIntent intent,
                                         String channelName,
                                         String topicName,
                                         Integer contentWordCount) {
        String rawText = chat(buildDraftPrompt(query, intent, channelName, topicName, contentWordCount));
        try {
            PublishDraftResult result = parseObject(rawText, PublishDraftResult.class);
            normalizeDraft(result);
            return result;
        } catch (Exception e) {
            log.warn("发布助手草稿 JSON 不合规，尝试修复: {}", rawText, e);
            PublishDraftResult result = parseObject(repairJson(rawText, "PublishDraftResult"), PublishDraftResult.class);
            normalizeDraft(result);
            return result;
        }
    }

    private String buildExtractPrompt(String query, List<CatalogItemResponse> channels) {
        return """
                你是知识分享社区的 AI 发布助手。请从用户输入中抽取发布笔记需要的信息。

                严格返回格式：
                只返回一个合法 JSON 对象，不要 Markdown，不要代码块，不要解释。
                字段名必须完全一致：
                {
                  "publishIntent": true,
                  "channelName": "用户指定的频道名称，没有则为空字符串",
                  "topicName": "用户指定的发布话题名称，没有则为空字符串",
                  "title": "用户明确指定的标题，没有则为空字符串",
                  "contentIdea": "用户真正想写的文章主题或内容方向，没有则为空字符串",
                  "contentWordCount": null,
                  "tags": ["用户指定或可推断的标签，2到5个，没有则返回空数组"]
                }

                抽取规则：
                1. 只有用户明显想发布、创建、生成并发布笔记时，publishIntent 才为 true；
                2. channelName 必须尽量匹配可选频道名称，不确定就返回空字符串；
                3. topicName 是频道下的发布话题名称，用户没明确指定就返回空字符串；
                4. tags 是内容标签，不等同于频道或话题；
                5. contentIdea 只来自用户明确表达的正文主题、写作对象或文章方向，不要从频道名、发布话题名反推；
                6. contentWordCount 只抽取用户明确指定的正文字数，例如“写800字”“正文1200字左右”返回 800 或 1200；不要抽取标题字数；
                7. 不要编造用户没有指定的频道、发布话题、文章主题和正文字数。

                可选频道：
                %s

                用户输入：
                %s
                """.formatted(formatCatalog(channels), query);
    }

    private String buildImagePrompt(String query, PublishAssistantIntent intent) {
        return """
                请为一篇知识分享社区图文笔记生成 3 张配图提示词。

                严格返回格式：
                只返回一个合法 JSON 对象，不要 Markdown，不要代码块，不要解释。
                {
                  "prompts": ["图片提示词1", "图片提示词2", "图片提示词3"]
                }

                提示词要求：
                1. 必须是中文；
                2. 每条提示词描述一张真实、干净、适合图文笔记封面或正文配图的画面；
                3. 三张图要围绕同一主题但视角不同；
                4. 不要出现文字排版、海报文字、水印、logo、品牌名；
                5. 不要包含夸张、猎奇、低俗或营销感元素。

                用户输入：
                %s

                已抽取内容方向：
                %s
                """.formatted(query, StrUtil.blankToDefault(intent.getContentIdea(), "暂无"));
    }

    private String buildDraftPrompt(String query,
                                    PublishAssistantIntent intent,
                                    String channelName,
                                    String topicName,
                                    Integer contentWordCount) {
        return """
                你是知识分享社区的中文笔记发布编辑。请根据用户输入的主题和需求，生成可直接发布的图文笔记草稿。
                图片只作为发布配图，不是标题或正文的信息来源。
                频道和发布话题只用于确认发布位置，不是正文主题来源。

                严格返回格式：
                只返回一个合法 JSON 对象，不要 Markdown，不要代码块，不要解释。
                字段名必须完全一致：
                {
                  "title": "12到24个中文字符标题",
                  "summary": "一句话概括这篇笔记，20到50个中文字符",
                  "content": "正文，可用\\n自然分段，不要Markdown标题、编号或项目符号",
                  "tags": ["2到6个中文字符标签", "2到6个中文字符标签", "2到6个中文字符标签"]
                }

                内容要求：
                1. 标题自然清晰，不要夸张营销；
                2. 正文必须可以直接作为图文笔记发布；
                3. 标题和正文必须直接围绕用户输入的文章主题生成，不要从图片内容提取或推断；
                4. 图片只用于配图，不要描述“图中出现了什么”，除非用户在文字中明确要求；
                5. 语气真诚、具体、有信息价值；
                6. tags 不能包含 #，不能直接重复频道名和话题名；
                7. 内容主题优先级必须是：用户输入的文章主题 > contentIdea；
                8. 禁止把频道名或发布话题名当作正文主题扩写，除非用户明确说文章就写这个。
                9. %s

                用户输入：
                %s

                抽取结果：
                发布频道（仅用于发布位置）：%s
                发布话题（仅用于发布位置）：%s
                标题草稿：%s
                内容方向：%s
                目标正文字数：%s
                标签：%s
                """.formatted(
                buildWordCountRequirement(contentWordCount),
                query,
                channelName,
                topicName,
                StrUtil.blankToDefault(intent.getTitle(), "暂无"),
                StrUtil.blankToDefault(intent.getContentIdea(), "暂无"),
                contentWordCount == null ? "默认300到500字" : contentWordCount + "字左右",
                intent.getTags()
        );
    }

    private String buildWordCountRequirement(Integer contentWordCount) {
        if (contentWordCount == null) {
            return "用户未指定正文字数，正文控制在300到500个中文字符。";
        }
        return "用户指定正文字数为" + contentWordCount + "字左右，必须尽量贴近该长度，且绝不能超过2000个中文字符。";
    }

    private String repairJson(String rawText, String schemaName) {
        return chat("""
                请把下面内容修复为合法 JSON。
                只返回 JSON，不要 Markdown，不要代码块，不要解释。
                schemaName: %s

                待修复内容：
                %s
                """.formatted(schemaName, StrUtil.blankToDefault(rawText, "")));
    }

    private String chat(String prompt) {
        ChatResponse response = chatModel.chat(UserMessage.from(prompt));
        return response.aiMessage().text();
    }

    private <T> T parseObject(String rawText, Class<T> clazz) {
        String json = extractJsonObject(rawText);
        T result = JsonUtil.parseObject(json, clazz);
        if (result == null) {
            throw new IllegalArgumentException("模型返回结果为空");
        }
        return result;
    }

    private String extractJsonObject(String rawText) {
        if (StrUtil.isBlank(rawText)) {
            throw new IllegalArgumentException("模型返回为空");
        }
        String text = rawText.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("模型返回不是合法 JSON 对象");
        }
        return text.substring(start, end + 1);
    }

    private String formatCatalog(List<CatalogItemResponse> items) {
        if (items == null || items.isEmpty()) {
            return "暂无";
        }
        StringBuilder builder = new StringBuilder();
        for (CatalogItemResponse item : items) {
            if (item != null && StrUtil.isNotBlank(item.getName())) {
                builder.append("- ").append(item.getName()).append("\n");
            }
        }
        return builder.toString();
    }

    private void normalizeIntent(PublishAssistantIntent intent) {
        intent.setPublishIntent(Boolean.TRUE.equals(intent.getPublishIntent()));
        intent.setChannelName(cleanLine(intent.getChannelName()));
        intent.setTopicName(cleanLine(intent.getTopicName()));
        intent.setTitle(limit(cleanLine(intent.getTitle()), 30));
        intent.setContentIdea(limit(cleanLine(intent.getContentIdea()), 120));
        if (intent.getContentWordCount() != null && intent.getContentWordCount() <= 0) {
            intent.setContentWordCount(null);
        }
        intent.setTags(normalizeList(intent.getTags(), 5, 8));
    }

    private void normalizeDraft(PublishDraftResult result) {
        result.setTitle(limit(cleanLine(result.getTitle()), 30));
        result.setSummary(limit(cleanLine(result.getSummary()), 60));
        result.setContent(limit(cleanContent(result.getContent()), 2000));
        result.setTags(normalizeList(result.getTags(), 5, 8));
        if (StrUtil.isBlank(result.getTitle()) || StrUtil.isBlank(result.getContent())) {
            throw new IllegalArgumentException("发布草稿缺少标题或正文");
        }
    }

    private List<String> normalizeList(List<String> values, int maxSize, int maxLength) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::cleanLine)
                .filter(StrUtil::isNotBlank)
                .map(value -> limit(value.replace("#", ""), maxLength))
                .limit(maxSize)
                .toList();
    }

    private String cleanLine(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return value.replaceAll("[\\r\\n]+", " ")
                .replaceAll("^[-*#\\d.、\\s]+", "")
                .trim();
    }

    private String cleanContent(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return value.replaceAll("(?m)^\\s*#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s*[-*]\\s+", "")
                .replaceAll("(?m)^\\s*\\d+[.、]\\s+", "")
                .trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
