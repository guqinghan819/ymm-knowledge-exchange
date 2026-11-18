package wcg.yhm.ai.biz.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.ImageWritingPolishAgent;
import wcg.yhm.ai.biz.agent.PublishAssistantAgent;
import wcg.yhm.ai.biz.agent.PublishAssistantPolishAgent;
import wcg.yhm.ai.biz.agent.WebSearchAgent;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.NoteContentPolishAgent;
import wcg.yhm.ai.biz.agent.pptx.PptIntentRecognizer;
import wcg.yhm.ai.biz.agent.result.PublishAssistantIntent;
import wcg.yhm.ai.biz.agent.result.PublishDraftResult;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStatusStrategyFactory;
import wcg.yhm.ai.biz.agent.pptx.strategy.impl.SchemaStrategy;
import wcg.yhm.ai.biz.agent.result.ImageWritingResult;
import wcg.yhm.ai.biz.agent.result.PptIntentResult;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.pojo.AiConversation;
import wcg.yhm.ai.biz.domain.pojo.AiMessage;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.domain.request.AiWriteContentRequest;
import wcg.yhm.ai.biz.domain.request.AiWriteTitleRequest;
import wcg.yhm.ai.biz.domain.request.ImageWritingRequest;
import wcg.yhm.ai.biz.domain.request.PublishAssistantRequest;
import wcg.yhm.ai.biz.manager.TaskManager;
import wcg.yhm.ai.biz.agent.tool.RecommendTool;
import wcg.yhm.ai.biz.agent.tool.TitleGenerator;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.note.api.controller.NoteFeignApi;
import wcg.yhm.note.api.domain.request.PublishNoteRequest;
import wcg.yhm.note.api.domain.request.QueryTopicRequest;
import wcg.yhm.note.api.domain.response.CatalogItemResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-08
 * @Description:
 * @Version: 1.0
 */

@Service
@Slf4j
public class AiService {

    @Resource
    private WebSearchAgent webSearchAgent;

    @Resource
    private RecommendTool recommendTool;

    @Resource
    private TitleGenerator titleGenerator;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Resource
    private TaskManager taskManager;

    @Resource
    private AiConversationRecordService conversationRecordService;

    @Resource
    private IAiMessageService aiMessageService;

    @Resource
    private IAiConversationService aiConversationService;

    @Resource
    private ChatMemoryProvider chatMemoryProvider;

    @Resource
    private PptIntentRecognizer pptIntentRecognizer;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    private PptStatusStrategyFactory pptStatusStrategyFactory;

    @Resource
    private ChatModel qwenVisionChatModel;

    @Resource
    private NoteContentPolishAgent noteContentPolishAgent;

    @Resource
    private ImageWritingPolishAgent imageWritingPolishAgent;

    @Resource
    private PublishAssistantAgent publishAssistantAgent;

    @Resource
    private PublishAssistantPolishAgent publishAssistantPolishAgent;

    @Resource
    private NoteFeignApi noteFeignApi;

    @Resource
    private PublishImageCoordinator publishImageCoordinator;


    private static final int MAX_SIZE = 7;
    private static final int PUBLISH_ASSISTANT_IMAGE_COUNT = 3;
    private static final int MAX_PUBLISH_ASSISTANT_CONTENT_WORD_COUNT = 2000;
    private static final Pattern SPECIFIC_CONTENT_WORD_COUNT_PATTERN = Pattern.compile("(?:正文|内容|文章|笔记)[^\\d]{0,12}(\\d{2,6})\\s*(?:个?字|字符)");
    private static final Pattern GENERAL_WORD_COUNT_PATTERN = Pattern.compile("(\\d{2,6})\\s*(?:个?字|字符)");
    private static final String TYPE_PUBLISH_OPTIONS = "publishOptions";
    private static final String TYPE_PUBLISH_THEME_OPTIONS = "publishThemeOptions";

    /**
     * 联网搜索
     */
    public Flux<String> webSearch(String conversationId, String query) {
        Long userId = UserContextHolder.getUserId();
        RecordingSink sink = conversationRecordService.begin(userId, conversationId, query, "webSearchAgent", Map.of());
        String taskId = sink.taskId();
        taskManager.register(taskId, sink);
        return sink.start(() -> {
            // Per-turn memory prevents late callbacks from changing another request's memory.
            String memoryId = buildMemoryId(userId, conversationId) + ":" + taskId;
            ChatMemory memory = chatMemoryProvider.get(memoryId);
            addMessages2Memory(memory, getHistoryMessages(userId, conversationId));
            TokenStream stream = webSearchAgent.stream(memoryId, query);
            StringBuilder references = new StringBuilder();
            stream.onPartialThinking(partial -> {
                if (!sink.isEnded()) sendEvent(sink, EventType.TYPE_THINKING, partial.text());
            });
            stream.onPartialResponseWithContext((partial, context) -> {
                taskManager.bindHandle(taskId, context.streamingHandle());
                if (!sink.isEnded()) sendEvent(sink, EventType.TYPE_TEXT, partial.text());
            });
            stream.onToolExecuted(tool -> {
                if (!sink.isEnded() && "searchWeb".equals(tool.request().name())) {
                    references.append(tool.result());
                    sendEvent(sink, EventType.TYPE_REFERENCE, tool.result());
                }
            });
            stream.onCompleteResponse(response -> {
                if (sink.isEnded()) return;
                try {
                    List<String> recommendations = generateRecommendations(query, references.toString());
                    if (CollUtil.isNotEmpty(recommendations)) sendEvent(sink, EventType.TYPE_RECOMMEND, recommendations);
                    sink.tryEmitComplete();
                } catch (Exception error) { sink.tryEmitError(error); }
            });
            stream.onError(sink::tryEmitError);
            stream.start();
        });
    }

    /**
     * 根据图文笔记首图生成标题
     */
    public Flux<String> writeNoteTitle(AiWriteTitleRequest request) {
        Sinks.Many<String> sink = Sinks.many().unicast()
                .onBackpressureBuffer(new LinkedBlockingQueue<>(64));

        Long userId = UserContextHolder.getUserId();
        String conversationId = StrUtil.blankToDefault(
                request.getConversationId(),
                UUID.randomUUID().toString()
        );
        String taskKey = buildMemoryId(userId, conversationId);
        taskManager.register(taskKey, sink);

        CompletableFuture.runAsync(() -> {
            try {
                if (taskManager.isCancelled(taskKey)) {
                    sink.tryEmitComplete();
                    return;
                }

                List<String> imageUrls = request.getImageUrls().stream()
                        .filter(StrUtil::isNotBlank)
                        .map(String::trim)
                        .toList();
                if (CollUtil.isEmpty(imageUrls)) {
                    sendEvent(sink, EventType.TYPE_ERROR, "图片地址列表不能为空");
                    sink.tryEmitComplete();
                    return;
                }

                sendEvent(sink, EventType.TYPE_STEP, "已收到" + imageUrls.size() + "张图片，准备解析...");
                List<String> imageSummaries = new ArrayList<>();

                for (int i = 0; i < imageUrls.size(); i++) {
                    if (taskManager.isCancelled(taskKey)) {
                        sink.tryEmitComplete();
                        return;
                    }

                    int imageIndex = i + 1;
                    sendEvent(sink, EventType.TYPE_STEP, "第" + imageIndex + "张图片正在解析...");
                    String summary = recognizeImageForTitle(imageUrls.get(i), imageIndex);
                    imageSummaries.add("第" + imageIndex + "张：" + summary);
                    sendEvent(sink, EventType.TYPE_STEP, "第" + imageIndex + "张图片解析完成");
                }

                sendEvent(sink, EventType.TYPE_STEP, "正在总结生成标题...");
                String title = generateTitleFromImageSummaries(imageSummaries, request);

                if (StrUtil.isBlank(title)) {
                    sendEvent(sink, EventType.TYPE_ERROR, "AI暂未生成标题，请稍后重试");
                    sink.tryEmitComplete();
                    return;
                }

                sendEvent(sink, EventType.TYPE_STEP, "标题生成完成");
                sendEvent(sink, EventType.TYPE_TEXT, title);
                sendEvent(sink, "complete", "done");
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("AI 写标题失败: imageUrls={}", request.getImageUrls(), e);
                sendEvent(sink, EventType.TYPE_ERROR,
                        e.getMessage() != null ? e.getMessage() : "AI写标题失败");
                sink.tryEmitComplete();
            } finally {
                taskManager.removeTask(taskKey);
            }
        }, taskExecutor);

        return sink.asFlux();
    }

    private String recognizeImageForTitle(String imageUrl, int imageIndex) {
        UserMessage userMessage = UserMessage.from(
                TextContent.from("""
                        请解析这张图文笔记图片，提取对标题有用的信息。
                        要求：
                        1. 用中文输出一句简短总结；
                        2. 重点说明图片主体、场景、动作、氛围；
                        3. 不要编造图片中没有的信息。
                        当前图片序号：第%s张
                        """.formatted(imageIndex)),
                ImageContent.from(imageUrl)
        );
        ChatResponse response = qwenVisionChatModel.chat(userMessage);
        return response.aiMessage().text();
    }

    private String generateTitleFromImageSummaries(List<String> imageSummaries, AiWriteTitleRequest request) {
        UserMessage userMessage = UserMessage.from(buildNoteTitlePrompt(imageSummaries, request));
        ChatResponse response = qwenVisionChatModel.chat(userMessage);
        return normalizeTitle(response.aiMessage().text());
    }

    private String buildNoteTitlePrompt(List<String> imageSummaries, AiWriteTitleRequest request) {
        return """
                你是知识分享社区的中文标题助手。请根据多张图片的解析结果，生成一个适合发布的中文标题。
                要求：
                1. 只输出一个标题，不要解释，不要加引号；
                2. 标题必须在20个汉字以内；
                3. 标题要自然、有吸引力，不能夸张营销；
                4. 综合所有图片的共同主题、主体、场景和氛围；
                5. 如果已有草稿，可作为辅助信息，但图片内容优先。

                图片解析结果：
                %s

                已有标题草稿：%s
                已有正文草稿：%s
                """.formatted(
                String.join("\n", imageSummaries),
                StrUtil.blankToDefault(request.getTitle(), "暂无"),
                StrUtil.blankToDefault(request.getContent(), "暂无")
        );
    }

    private String normalizeTitle(String title) {
        if (StrUtil.isBlank(title)) {
            return "";
        }
        String normalized = title
                .replaceAll("^[\\s\"“”'‘’]+|[\\s\"“”'‘’]+$", "")
                .replaceAll("^\\d+[.、]\\s*", "")
                .split("\\R")[0]
                .trim();
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }

    /**
     * 根据图文笔记图片列表生成正文
     */
    public Flux<String> writeNoteContent(AiWriteContentRequest request) {
        Sinks.Many<String> sink = Sinks.many().unicast()
                .onBackpressureBuffer(new LinkedBlockingQueue<>(64));

        Long userId = UserContextHolder.getUserId();
        String conversationId = StrUtil.blankToDefault(
                request.getConversationId(),
                UUID.randomUUID().toString()
        );
        String taskKey = buildMemoryId(userId, conversationId);
        taskManager.register(taskKey, sink);

        CompletableFuture.runAsync(() -> {
            try {
                List<String> imageUrls = request.getImageUrls().stream()
                        .filter(StrUtil::isNotBlank)
                        .map(String::trim)
                        .toList();
                if (CollUtil.isEmpty(imageUrls)) {
                    sendEvent(sink, EventType.TYPE_ERROR, "图片地址列表不能为空");
                    sink.tryEmitComplete();
                    return;
                }

                sendEvent(sink, EventType.TYPE_STEP, "已收到" + imageUrls.size() + "张图片，准备解析...");
                List<String> imageSummaries = new ArrayList<>();

                for (int i = 0; i < imageUrls.size(); i++) {
                    if (taskManager.isCancelled(taskKey)) {
                        sink.tryEmitComplete();
                        return;
                    }

                    int imageIndex = i + 1;
                    sendEvent(sink, EventType.TYPE_STEP, "第" + imageIndex + "张图片正在解析...");
                    String summary = recognizeImageForContent(imageUrls.get(i), imageIndex);
                    imageSummaries.add("第" + imageIndex + "张：" + summary);
                    sendEvent(sink, EventType.TYPE_STEP, "第" + imageIndex + "张图片解析完成");
                }

                sendEvent(sink, EventType.TYPE_STEP, "正在提炼知识分享要点...");
                List<String> knowledgeSummaries = summarizeKnowledgePoints(imageSummaries, request);

                sendEvent(sink, EventType.TYPE_STEP, "正在润色生成正文...");
                String content = normalizeContent(noteContentPolishAgent.polish(knowledgeSummaries, request));

                if (StrUtil.isBlank(content)) {
                    sendEvent(sink, EventType.TYPE_ERROR, "AI暂未生成正文，请稍后重试");
                    sink.tryEmitComplete();
                    return;
                }

                sendEvent(sink, EventType.TYPE_STEP, "正文生成完成");
                sendEvent(sink, EventType.TYPE_TEXT, content);
                sendEvent(sink, "complete", "done");
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("AI 写正文失败: imageUrls={}", request.getImageUrls(), e);
                sendEvent(sink, EventType.TYPE_ERROR,
                        e.getMessage() != null ? e.getMessage() : "AI写正文失败");
                sink.tryEmitComplete();
            } finally {
                taskManager.removeTask(taskKey);
            }
        }, taskExecutor);

        return sink.asFlux();
    }

    private String recognizeImageForContent(String imageUrl, int imageIndex) {
        UserMessage userMessage = UserMessage.from(
                TextContent.from("""
                        请解析这张图文笔记图片，为知识分享正文提取素材。
                        要求：
                        1. 用中文输出一段简洁总结；
                        2. 说明图片里的主体、场景、细节、可能传递的信息；
                        3. 提取可以分享给读者的观察、经验或方法；
                        4. 不要编造图片中无法推断的事实。
                        当前图片序号：第%s张
                        """.formatted(imageIndex)),
                ImageContent.from(imageUrl)
        );
        ChatResponse response = qwenVisionChatModel.chat(userMessage);
        return response.aiMessage().text();
    }

    private List<String> summarizeKnowledgePoints(List<String> imageSummaries, AiWriteContentRequest request) {
        UserMessage userMessage = UserMessage.from("""
                请根据多张图片解析结果，提炼适合知识分享社区正文的要点。
                要求：
                1. 输出3到5条中文要点；
                2. 每条要点要有信息价值，可帮助读者理解、学习或获得启发；
                3. 不要输出标题，不要解释任务过程；
                4. 不要编造图片中无法推断的事实。

                图片解析结果：
                %s

                当前标题：%s
                已有正文草稿：%s
                """.formatted(
                String.join("\n", imageSummaries),
                StrUtil.blankToDefault(request.getTitle(), "暂无"),
                StrUtil.blankToDefault(request.getContent(), "暂无")
        ));
        ChatResponse response = qwenVisionChatModel.chat(userMessage);
        return Arrays.stream(response.aiMessage().text().split("\\R"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private String normalizeContent(String content) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        String normalized = content
                .replaceAll("^[\\s\"“”'‘’]+|[\\s\"“”'‘’]+$", "")
                .trim();
        return normalized.length() > 6000 ? normalized.substring(0, 6000) : normalized;
    }

    /**
     * AI 对话页识图写作：保存本轮图文记录，模型仍只处理本次图片与写作要求。
     */
    public Flux<String> imageWriting(ImageWritingRequest request) {
        Long userId = UserContextHolder.getUserId();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("imageUrls", normalizeImageUrls(request.getImageUrls()));
        RecordingSink sink = conversationRecordService.begin(userId, request.getConversationId(),
                StrUtil.blankToDefault(request.getQuery(), "请根据图片生成一段知识分享正文"), "imageWritingAgent", metadata);
        String taskKey = sink.taskId();
        taskManager.register(taskKey, sink);

        return sink.start(() -> CompletableFuture.runAsync(() -> {
            try {
                List<String> imageUrls = Optional.ofNullable(request.getImageUrls())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(StrUtil::isNotBlank)
                        .map(String::trim)
                        .toList();
                if (CollUtil.isEmpty(imageUrls)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_ERROR, "图片地址列表不能为空");
                    sink.tryEmitComplete();
                    return;
                }

                String query = StrUtil.blankToDefault(request.getQuery(), "请根据图片生成一段知识分享正文");
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_THINKING,
                        "已收到" + imageUrls.size() + "张图片地址，开始识图写作。\n");

                List<String> imageSummaries = new ArrayList<>();
                for (int i = 0; i < imageUrls.size(); i++) {
                    if (taskManager.isCancelled(taskKey)) {
                        sink.tryEmitComplete();
                        return;
                    }

                    int imageIndex = i + 1;
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING, "正在解析第" + imageIndex + "张图片...\n");
                    String summary = recognizeImageForImageWriting(imageUrls.get(i), imageIndex, query);
                    imageSummaries.add("第" + imageIndex + "张：" + summary);
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "第" + imageIndex + "张图片解析完成：\n" + summary + "\n");
                }

                if (taskManager.isCancelled(taskKey)) {
                    sink.tryEmitComplete();
                    return;
                }

                if (sink.isEnded()) return;

                sendEvent(sink, EventType.TYPE_THINKING, "正在梳理主题、素材取舍与写作结构...\n");
                String writingPlan = buildImageWritingPlan(imageSummaries, query);
                if (StrUtil.isNotBlank(writingPlan)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING, "写作过程摘要：\n" + writingPlan + "\n");
                }

                if (sink.isEnded()) return;

                sendEvent(sink, EventType.TYPE_THINKING, "正在润色并整理最终结果...\n");
                ImageWritingResult result = imageWritingPolishAgent.polish(imageSummaries, writingPlan, query);
                String content = formatImageWritingResult(result);
                if (StrUtil.isBlank(content)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_ERROR, "AI暂未生成结果，请稍后重试");
                    sink.tryEmitComplete();
                    return;
                }

                if (sink.isEnded()) return;

                sendEvent(sink, EventType.TYPE_THINKING, "最终结果已按固定格式整理完成。\n");
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_TEXT, content);
                if (sink.isEnded()) return;
                sendEvent(sink, "complete", "done");
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("AI 识图写作失败: imageUrls={}, query={}", request.getImageUrls(), request.getQuery(), e);
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_ERROR,
                        e.getMessage() != null ? e.getMessage() : "AI识图写作失败");
                sink.tryEmitComplete();
            } finally {
                taskManager.removeTask(taskKey);
            }
        }, taskExecutor));
    }

    private String recognizeImageForImageWriting(String imageUrl, int imageIndex, String query) {
        UserMessage userMessage = UserMessage.from(
                TextContent.from("""
                        请解析这张图片，为后续中文写作提取可靠素材。
                        要求：
                        1. 用中文输出一段简洁总结；
                        2. 说明图片中的主体、场景、细节、动作或关系；
                        3. 提取可用于知识分享、观察记录或文案表达的素材；
                        4. 结合用户写作要求判断哪些画面信息更重要；
                        5. 不要编造图片中无法判断的人名、地点、品牌、时间、数据、功效和因果关系。

                        当前图片序号：第%s张
                        用户写作要求：%s
                        """.formatted(imageIndex, query)),
                ImageContent.from(imageUrl)
        );
        ChatResponse response = qwenVisionChatModel.chat(userMessage);
        return response.aiMessage().text();
    }

    private String formatImageWritingResult(ImageWritingResult result) {
        if (result == null) {
            return "";
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(result.getTitle()).append("\n\n");
        markdown.append("> ").append(result.getSummary()).append("\n\n");
        markdown.append("## 正文\n\n");
        markdown.append(result.getContent()).append("\n\n");

        if (CollUtil.isNotEmpty(result.getHighlights())) {
            markdown.append("## 内容要点\n\n");
            for (String highlight : result.getHighlights()) {
                if (StrUtil.isNotBlank(highlight)) {
                    markdown.append("- ").append(highlight.trim()).append("\n");
                }
            }
            markdown.append("\n");
        }

        if (CollUtil.isNotEmpty(result.getTags())) {
            markdown.append("## 推荐标签\n\n");
            for (String tag : result.getTags()) {
                if (StrUtil.isNotBlank(tag)) {
                    markdown.append("`#").append(tag.replace("#", "").trim()).append("` ");
                }
            }
        }

        return normalizeContent(markdown.toString());
    }

    /**
     * AI 发布助手：要素齐全后调用真实发布接口，否则停在补充信息阶段。
     */
    public Flux<String> publishAssistant(PublishAssistantRequest request) {
        Long userId = UserContextHolder.getUserId();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("imageUrls", normalizeImageUrls(request.getImageUrls()));
        metadata.put("channelId", request.getChannelId());
        metadata.put("channelName", request.getChannelName());
        metadata.put("topicId", request.getTopicId());
        metadata.put("topicName", request.getTopicName());
        RecordingSink sink = conversationRecordService.begin(userId, request.getConversationId(),
                StrUtil.blankToDefault(request.getQuery(), "请根据图片生成一段知识分享正文"), "publishAssistantAgent", metadata);
        String taskKey = sink.taskId();
        taskManager.register(taskKey, sink);

        return sink.start(() -> CompletableFuture.runAsync(() -> {
            PublishImageCoordinator.Batch imageBatch = null;
            try {
                UserContextHolder.setUserId(userId);
                String query = request.getQuery().trim();
                List<String> inputImageUrls = normalizeImageUrls(request.getImageUrls());
                log.info("发布助手开始: userId={}, conversationId={}, imageCount={}, query={}",
                        userId, request.getConversationId(), inputImageUrls.size(), query);

                if (sink.isEnded()) return;

                sendEvent(sink, EventType.TYPE_THINKING,
                        "### 发布助手执行链路\n"
                                + "- 已收到发布需求，开始读取频道配置。\n");

                List<CatalogItemResponse> channels = loadChannels();
                if (CollUtil.isEmpty(channels)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT,
                            "## 需要稍后再试\n\n暂时没有读取到可用频道，发布助手无法确认发布位置，因此不会继续生成或调用发布接口。");
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_THINKING,
                        "- 已读取 " + channels.size() + " 个频道，开始识别发布意图和缺失信息。\n");

                PublishAssistantIntent intent = publishAssistantAgent.extractIntent(query, channels);
                log.info("发布助手意图识别完成: {}", JsonUtil.toJsonString(intent));
                Integer contentWordCount = resolveContentWordCount(query, intent);
                CatalogItemResponse selectedChannel = findSelectedCatalogItem(
                        channels, request.getChannelId(), request.getChannelName());
                if (selectedChannel != null) {
                    intent.setChannelName(selectedChannel.getName());
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 已读取用户提前选择的频道：" + selectedChannel.getName() + "。\n");
                }

                if (!Boolean.TRUE.equals(intent.getPublishIntent())) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 未检测到明确发布意图，已停止，避免误发布。\n");
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT, formatNeedPublishIntentResponse(query));
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }

                if (contentWordCount != null && contentWordCount > MAX_PUBLISH_ASSISTANT_CONTENT_WORD_COUNT) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 用户指定的正文字数超过上限，已停止，未调用发布接口。\n");
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT, formatContentWordCountLimitResponse(contentWordCount));
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }
                if (contentWordCount != null) {
                    intent.setContentWordCount(contentWordCount);
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 已识别目标正文字数：" + contentWordCount + " 字，未超过 2000 字上限。\n");
                }

                if (isContentThemeMissing(intent, inputImageUrls)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 未检测到明确文章主题，已停止生成，等待用户补充写作方向。\n");
                    if (sink.isEnded()) return;
                    sendEvent(sink, TYPE_PUBLISH_THEME_OPTIONS, buildPublishThemeOptions());
                    if (StrUtil.isBlank(intent.getChannelName())) {
                        if (sink.isEnded()) return;
                        sendEvent(sink, TYPE_PUBLISH_OPTIONS, buildPublishOptionGroups(channels));
                    }
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT, formatNeedContentThemeResponse(query, intent));
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }

                CatalogItemResponse channel = selectedChannel != null
                        ? selectedChannel
                        : findCatalogItem(channels, intent.getChannelName());
                if (channel == null) {
                    List<Map<String, Object>> optionGroups = buildPublishOptionGroups(channels);
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 频道信息未命中，已读取频道和话题选项，等待用户选择。\n");
                    if (sink.isEnded()) return;
                    sendEvent(sink, TYPE_PUBLISH_OPTIONS, optionGroups);
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT, formatNeedChannelResponse(query, intent, optionGroups));
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_THINKING,
                        "- 已确认频道：" + channel.getName() + "，开始读取该频道下的话题。\n");

                List<CatalogItemResponse> topics = loadTopics(channel.getId());
                if (CollUtil.isEmpty(topics)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT,
                            "## 需要补充话题\n\n已确认频道 `" + channel.getName()
                                    + "`，但没有读取到该频道下可选的话题。为避免误发布，当前流程已停止。");
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }

                CatalogItemResponse selectedTopic = findSelectedCatalogItem(
                        topics, request.getTopicId(), request.getTopicName());
                if (selectedTopic != null) {
                    intent.setTopicName(selectedTopic.getName());
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 已读取用户提前选择的话题：" + selectedTopic.getName() + "。\n");
                }

                CatalogItemResponse topic = selectedTopic != null
                        ? selectedTopic
                        : findCatalogItem(topics, intent.getTopicName());
                if (topic == null) {
                    List<Map<String, Object>> optionGroups = buildPublishOptionGroups(channel, topics);
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 话题信息未命中，已准备该频道下的话题选项，等待用户选择。\n");
                    if (sink.isEnded()) return;
                    sendEvent(sink, TYPE_PUBLISH_OPTIONS, optionGroups);
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT, formatNeedTopicResponse(query, intent, channel, topics));
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_THINKING,
                        "- 已确认话题：" + topic.getName() + "，笔记位置要素齐全。\n");

                List<String> publishImageUrls = new ArrayList<>(inputImageUrls);
                if (CollUtil.isEmpty(publishImageUrls)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 未检测到用户上传图片，将自动生成 3 张配图。\n");
                    List<String> generatedPrompts;
                    try {
                        generatedPrompts = publishAssistantAgent.buildImagePrompts(query, intent);
                    } catch (Exception e) {
                        log.warn("配图提示词生成失败，使用主题提示词", e);
                        generatedPrompts = List.of();
                    }
                    if (sink.isEnded()) return;
                    imageBatch = publishImageCoordinator.start(fillImagePrompts(generatedPrompts, query), userId, sink);
                    sendEvent(sink, EventType.TYPE_THINKING, "- 三张配图并行处理，同时准备笔记草稿。\n");
                } else {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 检测到用户已上传 " + publishImageUrls.size() + " 张图片，图片将仅作为配图使用。\n");
                }

                if (sink.isEnded()) return;

                sendEvent(sink, EventType.TYPE_THINKING,
                        "- 正在根据用户主题生成标题和正文，图片不参与正文提取。\n");
                PublishDraftResult draft = publishAssistantAgent.buildDraft(
                        query, intent, channel.getName(), topic.getName(), contentWordCount);
                log.info("发布助手草稿生成完成: title={}, contentLength={}, tags={}",
                        draft.getTitle(), draft.getContent() == null ? 0 : draft.getContent().length(), draft.getTags());

                if (sink.isEnded()) return;

                sendEvent(sink, EventType.TYPE_THINKING,
                        "- 正在调用发布润色助手，优化标题、摘要和正文。\n");
                draft = publishAssistantPolishAgent.polish(
                        query, intent, draft, channel.getName(), topic.getName(), contentWordCount);
                List<String> tags = CollUtil.isNotEmpty(draft.getTags()) ? draft.getTags() : intent.getTags();
                log.info("发布助手润色完成: title={}, contentLength={}, tags={}",
                        draft.getTitle(), draft.getContent() == null ? 0 : draft.getContent().length(), tags);

                if (imageBatch != null) {
                    publishImageUrls.addAll(publishImageCoordinator.await(imageBatch, sink));
                    if (sink.isEnded()) return;
                    sink.metadata("generatedImageUrls", publishImageUrls);
                    if (publishImageUrls.isEmpty()) {
                        sendEvent(sink, EventType.TYPE_TEXT,
                                "## 未发布：配图生成失败\n\n三张配图均未成功生成或上传，请添加图片后重试。\n\n"
                                        + "### 已准备的草稿\n\n# " + draft.getTitle() + "\n\n" + draft.getContent());
                        sink.tryEmitComplete();
                        return;
                    }
                    if (publishImageUrls.size() < PUBLISH_ASSISTANT_IMAGE_COUNT) {
                        sendEvent(sink, EventType.TYPE_THINKING,
                                "- 已成功获取 " + publishImageUrls.size() + "/3 张配图，将按实际张数发布。\n");
                    }
                }

                List<String> missingFields = validatePublishAssistantReady(intent, draft, publishImageUrls, channel, topic);
                if (CollUtil.isNotEmpty(missingFields)) {
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_THINKING,
                            "- 发布必要要素校验未通过，已停止，未调用发布接口。\n");
                    if (sink.isEnded()) return;
                    sendEvent(sink, EventType.TYPE_TEXT, formatMissingPublishFields(missingFields, channel, topic));
                    if (sink.isEnded()) return;
                    sendEvent(sink, "complete", "done");
                    sink.tryEmitComplete();
                    return;
                }

                if (sink.isEnded()) return;

                sendEvent(sink, EventType.TYPE_THINKING,
                        "- 发布必要要素齐全，正在调用笔记发布接口。\n");
                PublishNoteRequest publishNoteRequest = buildPublishNoteRequest(draft, publishImageUrls, topic);
                if (sink.isEnded()) return;
                sink.metadata("publishOutcome", "SUBMITTING");
                Response<?> publishResponse = noteFeignApi.publishNote(publishNoteRequest);
                if (publishResponse == null || !publishResponse.isSuccess()) {
                    sink.metadata("publishOutcome", "FAILED");
                    String message = publishResponse == null || StrUtil.isBlank(publishResponse.getMessage())
                            ? "发布接口调用失败"
                            : publishResponse.getMessage();
                    throw new IllegalStateException(message);
                }
                if (sink.isEnded()) return;
                sink.metadata("publishOutcome", "SUCCESS");
                sendEvent(sink, EventType.TYPE_THINKING,
                        "- 笔记已提交发布接口，等待审核通过后展示。\n");
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_TEXT, formatPublishSuccess(draft, channel, topic, publishImageUrls));
                if (sink.isEnded()) return;
                sendEvent(sink, "complete", "done");
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("发布助手执行失败: conversationId={}, query={}",
                        request.getConversationId(), request.getQuery(), e);
                if (sink.isEnded()) return;
                sendEvent(sink, EventType.TYPE_ERROR,
                        e.getMessage() != null ? e.getMessage() : "发布助手执行失败");
                sink.tryEmitComplete();
            } finally {
                if (imageBatch != null) imageBatch.cancel();
                taskManager.removeTask(taskKey);
                UserContextHolder.removeThreadLocal();
            }
        }, taskExecutor));
    }

    private List<CatalogItemResponse> loadChannels() {
        Response<List<CatalogItemResponse>> response = noteFeignApi.getChannels();
        if (response == null || !response.isSuccess()) {
            throw new IllegalStateException("频道信息读取失败");
        }
        return Optional.ofNullable(response.getData()).orElse(Collections.emptyList());
    }

    private List<CatalogItemResponse> loadTopics(Long channelId) {
        Response<List<CatalogItemResponse>> response = noteFeignApi.getTopics(new QueryTopicRequest(channelId));
        if (response == null || !response.isSuccess()) {
            throw new IllegalStateException("话题信息读取失败");
        }
        return Optional.ofNullable(response.getData()).orElse(Collections.emptyList());
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        return Optional.ofNullable(imageUrls)
                .orElse(Collections.emptyList())
                .stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .limit(PUBLISH_ASSISTANT_IMAGE_COUNT)
                .toList();
    }

    private CatalogItemResponse findSelectedCatalogItem(List<CatalogItemResponse> items, Long id, String name) {
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        if (id != null) {
            return items.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> id.equals(item.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return findCatalogItem(items, name);
    }

    private CatalogItemResponse findCatalogItem(List<CatalogItemResponse> items, String name) {
        if (CollUtil.isEmpty(items) || StrUtil.isBlank(name)) {
            return null;
        }
        String normalizedName = normalizeCatalogName(name);
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.isNotBlank(item.getName()))
                .filter(item -> normalizeCatalogName(item.getName()).equals(normalizedName))
                .findFirst()
                .orElseGet(() -> items.stream()
                        .filter(Objects::nonNull)
                        .filter(item -> StrUtil.isNotBlank(item.getName()))
                        .filter(item -> {
                            String current = normalizeCatalogName(item.getName());
                            return current.contains(normalizedName) || normalizedName.contains(current);
                        })
                        .findFirst()
                        .orElse(null));
    }

    private String normalizeCatalogName(String name) {
        return StrUtil.blankToDefault(name, "")
                .replaceAll("[\\s#《》【】\\[\\]（）()]+", "")
                .toLowerCase(Locale.ROOT);
    }

    private List<String> fillImagePrompts(List<String> prompts, String query) {
        List<String> normalized = Optional.ofNullable(prompts).orElse(Collections.emptyList())
                .stream().filter(StrUtil::isNotBlank).map(String::trim)
                .limit(PUBLISH_ASSISTANT_IMAGE_COUNT).collect(Collectors.toCollection(ArrayList::new));
        String theme = trimToLength(query, 40);
        String[] compositions = {
                "主题主视觉，简洁真实，突出核心事物，横向构图",
                "相关场景与使用过程，带环境细节，横向构图",
                "关键细节特写，换一个视角和构图，横向构图"
        };
        while (normalized.size() < PUBLISH_ASSISTANT_IMAGE_COUNT) {
            normalized.add("围绕“" + theme + "”生成知识分享配图："
                    + compositions[normalized.size()] + "。无文字、无水印、构图清晰。");
        }
        return normalized;
    }

    private Integer resolveContentWordCount(String query, PublishAssistantIntent intent) {
        Integer extracted = extractContentWordCount(query);
        if (extracted != null) {
            return extracted;
        }
        if (intent == null || intent.getContentWordCount() == null || intent.getContentWordCount() <= 0) {
            return null;
        }
        return intent.getContentWordCount();
    }

    private Integer extractContentWordCount(String query) {
        if (StrUtil.isBlank(query)) {
            return null;
        }
        Matcher specificMatcher = SPECIFIC_CONTENT_WORD_COUNT_PATTERN.matcher(query);
        Integer wordCount = null;
        while (specificMatcher.find()) {
            wordCount = parsePositiveInt(specificMatcher.group(1));
        }
        if (wordCount != null) {
            return wordCount;
        }

        Matcher generalMatcher = GENERAL_WORD_COUNT_PATTERN.matcher(query);
        while (generalMatcher.find()) {
            wordCount = parsePositiveInt(generalMatcher.group(1));
        }
        return wordCount;
    }

    private Integer parsePositiveInt(String value) {
        try {
            int number = Integer.parseInt(value);
            return number > 0 ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> validatePublishAssistantReady(PublishAssistantIntent intent,
                                                       PublishDraftResult draft,
                                                       List<String> imageUrls,
                                                       CatalogItemResponse channel,
                                                       CatalogItemResponse topic) {
        List<String> missingFields = new ArrayList<>();
        if (intent == null || !Boolean.TRUE.equals(intent.getPublishIntent())) {
            missingFields.add("明确发布意图");
        }
        if (channel == null || channel.getId() == null) {
            missingFields.add("频道 ID");
        }
        if (topic == null || topic.getId() == null) {
            missingFields.add("话题 ID");
        }
        if (draft == null || StrUtil.isBlank(draft.getTitle())) {
            missingFields.add("标题");
        }
        if (draft == null || StrUtil.isBlank(draft.getContent())) {
            missingFields.add("正文");
        } else if (draft.getContent().length() > MAX_PUBLISH_ASSISTANT_CONTENT_WORD_COUNT) {
            missingFields.add("正文不能超过2000字");
        }
        if (CollUtil.isEmpty(imageUrls)) {
            missingFields.add("图片");
        } else if (imageUrls.size() > 8) {
            missingFields.add("图片数量不能超过8张");
        }
        return missingFields;
    }

    private PublishNoteRequest buildPublishNoteRequest(PublishDraftResult draft,
                                                       List<String> imageUrls,
                                                       CatalogItemResponse topic) {
        PublishNoteRequest request = new PublishNoteRequest();
        request.setType(0);
        request.setImageUrls(imageUrls);
        request.setTitle(draft.getTitle().trim());
        request.setContent(draft.getContent().trim());
        request.setTopicId(topic.getId());
        return request;
    }

    private boolean isContentThemeMissing(PublishAssistantIntent intent, List<String> inputImageUrls) {
        if (CollUtil.isNotEmpty(inputImageUrls)) {
            return false;
        }
        if (intent == null || StrUtil.isBlank(intent.getContentIdea())) {
            return true;
        }
        String contentIdea = normalizeCatalogName(intent.getContentIdea());
        Set<String> genericIdeas = Set.of(
                "发布笔记",
                "发布一篇笔记",
                "发笔记",
                "写笔记",
                "图文笔记",
                "知识分享",
                "文章",
                "正文",
                "暂无"
        );
        return genericIdeas.contains(contentIdea);
    }

    private List<String> buildPublishThemeOptions() {
        return List.of(
                "经验分享",
                "教程步骤",
                "避坑清单",
                "观点解读",
                "案例复盘",
                "作品展示"
        );
    }

    private List<Map<String, Object>> buildPublishOptionGroups(List<CatalogItemResponse> channels) {
        if (CollUtil.isEmpty(channels)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> optionGroups = new ArrayList<>();
        for (CatalogItemResponse channel : channels) {
            if (channel == null || channel.getId() == null || StrUtil.isBlank(channel.getName())) {
                continue;
            }
            List<CatalogItemResponse> topics;
            try {
                topics = loadTopics(channel.getId());
            } catch (Exception e) {
                log.warn("发布助手读取频道话题失败: channelId={}, channelName={}",
                        channel.getId(), channel.getName(), e);
                topics = Collections.emptyList();
            }
            optionGroups.add(buildPublishOptionGroup(channel, topics));
        }
        return optionGroups;
    }

    private List<Map<String, Object>> buildPublishOptionGroups(CatalogItemResponse channel,
                                                               List<CatalogItemResponse> topics) {
        if (channel == null) {
            return Collections.emptyList();
        }
        return List.of(buildPublishOptionGroup(channel, topics));
    }

    private Map<String, Object> buildPublishOptionGroup(CatalogItemResponse channel,
                                                        List<CatalogItemResponse> topics) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("channelId", channel.getId());
        group.put("channelName", channel.getName());

        List<Map<String, Object>> topicOptions = Optional.ofNullable(topics)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(topic -> topic.getId() != null && StrUtil.isNotBlank(topic.getName()))
                .map(topic -> {
                    Map<String, Object> topicOption = new LinkedHashMap<>();
                    topicOption.put("topicId", topic.getId());
                    topicOption.put("topicName", topic.getName());
                    return topicOption;
                })
                .toList();
        group.put("topics", topicOptions);
        return group;
    }

    private String formatNeedChannelResponse(String query,
                                             PublishAssistantIntent intent,
                                             List<Map<String, Object>> optionGroups) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("## 还差一步：请选择频道和话题\n\n");
        markdown.append("我已经理解你想发布的内容，但还不能确定发布位置。为避免误发布，当前不会继续生成或调用发布接口。\n\n");
        markdown.append("### 我理解到的内容\n");
        markdown.append("- 发布需求：").append(query).append("\n");
        if (intent != null && StrUtil.isNotBlank(intent.getContentIdea())) {
            markdown.append("- 内容方向：").append(intent.getContentIdea()).append("\n");
        }
        markdown.append("\n### 可选频道和话题\n");
        appendOptionGroups(markdown, optionGroups);
        markdown.append("\n也可以直接点击下方选项，我会按你选择的频道和话题继续。");
        return markdown.toString();
    }

    private String formatNeedContentThemeResponse(String query, PublishAssistantIntent intent) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("## 还差一步：请补充文章主题\n\n");
        markdown.append("我已经收到发布请求，但还不知道这篇笔记主要写什么。频道和话题只决定发布位置，不能替代正文主题，所以当前不会生成图片、正文或调用发布接口。\n\n");
        markdown.append("### 我现在缺少的信息\n");
        markdown.append("- 文章主题：例如写某个知识点、经历、教程、避坑点、案例或作品说明\n");
        markdown.append("- 正文字数：可以指定，例如 800 字；最多不能超过 2000 字\n");
        if (intent != null && StrUtil.isNotBlank(intent.getChannelName())) {
            markdown.append("- 已识别频道：").append(intent.getChannelName()).append("\n");
        }
        if (intent != null && StrUtil.isNotBlank(intent.getTopicName())) {
            markdown.append("- 已识别话题：").append(intent.getTopicName()).append("\n");
        }
        markdown.append("\n### 可以这样补充\n");
        markdown.append("- `写一篇关于 Spring Boot 接口限流的教程`\n");
        markdown.append("- `写一篇我整理前端性能优化经验的笔记`\n");
        markdown.append("- `写一篇新手学习 Java 集合的避坑清单`\n");
        markdown.append("\n你也可以点击下方文章方向，再补一句具体要写的内容。");
        return markdown.toString();
    }

    private String formatContentWordCountLimitResponse(Integer contentWordCount) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("## 正文字数超过上限\n\n");
        markdown.append("你指定的正文约为 ").append(contentWordCount).append(" 字，发布助手当前最多支持 2000 字正文。\n\n");
        markdown.append("为避免生成后超出限制，本次没有生成正文，也没有调用发布接口。\n\n");
        markdown.append("请把正文字数调整到 2000 字以内，例如：`正文写 1200 字左右`。");
        return markdown.toString();
    }

    private String formatNeedPublishIntentResponse(String query) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("## 请确认是否发布\n\n");
        markdown.append("我收到了你的内容，但没有识别到明确的发布意图。为避免误发布，当前没有生成或调用发布接口。\n\n");
        markdown.append("### 当前输入\n");
        markdown.append("- ").append(query).append("\n\n");
        markdown.append("你可以这样说：`帮我发布一篇关于 Spring Boot 接口限流的笔记，发到技术频道的话题 Java`。");
        return markdown.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendOptionGroups(StringBuilder markdown, List<Map<String, Object>> optionGroups) {
        if (CollUtil.isEmpty(optionGroups)) {
            markdown.append("- 暂无可选项\n");
            return;
        }
        for (Map<String, Object> group : optionGroups) {
            String channelName = String.valueOf(group.getOrDefault("channelName", ""));
            if (StrUtil.isBlank(channelName)) {
                continue;
            }
            markdown.append("- 频道：`").append(channelName).append("`\n");
            Object topicsValue = group.get("topics");
            if (!(topicsValue instanceof List<?> topics) || topics.isEmpty()) {
                markdown.append("  - 暂无话题\n");
                continue;
            }
            for (Object topicValue : topics) {
                if (topicValue instanceof Map<?, ?> topic) {
                    Object topicName = topic.get("topicName");
                    if (topicName != null && StrUtil.isNotBlank(String.valueOf(topicName))) {
                        markdown.append("  - 话题：`").append(topicName).append("`\n");
                    }
                }
            }
        }
    }

    private String formatNeedTopicResponse(String query,
                                           PublishAssistantIntent intent,
                                           CatalogItemResponse channel,
                                           List<CatalogItemResponse> topics) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("## 还差一步：请选择话题\n\n");
        markdown.append("频道 `").append(channel.getName()).append("` 已确认，但还需要选择该频道下的话题。话题未确认前不会继续生成或调用发布接口。\n\n");
        markdown.append("### 我理解到的内容\n");
        markdown.append("- 发布需求：").append(query).append("\n");
        if (intent != null && StrUtil.isNotBlank(intent.getContentIdea())) {
            markdown.append("- 内容方向：").append(intent.getContentIdea()).append("\n");
        }
        markdown.append("\n### 可选话题\n");
        for (CatalogItemResponse topic : topics) {
            if (topic != null && StrUtil.isNotBlank(topic.getName())) {
                markdown.append("- `").append(topic.getName()).append("`\n");
            }
        }
        markdown.append("\n你可以这样补充：`就发布到 ")
                .append(channel.getName())
                .append(" 频道，话题选 话题名称`。");
        return markdown.toString();
    }

    private String formatMissingPublishFields(List<String> missingFields,
                                              CatalogItemResponse channel,
                                              CatalogItemResponse topic) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("## 发布必要要素校验未通过\n\n");
        markdown.append("当前笔记信息还不完整，因此已停止，没有调用发布接口。\n\n");
        markdown.append("### 已确认\n");
        markdown.append("- 频道：").append(channel == null ? "未确认" : channel.getName()).append("\n");
        markdown.append("- 话题：").append(topic == null ? "未确认" : topic.getName()).append("\n\n");
        markdown.append("### 缺失信息\n");
        for (String field : missingFields) {
            markdown.append("- ").append(field).append("\n");
        }
        return markdown.toString();
    }

    private String formatPublishSuccess(PublishDraftResult draft,
                                        CatalogItemResponse channel,
                                        CatalogItemResponse topic,
                                        List<String> imageUrls) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# 发布助手发布成功\n\n");
        markdown.append("> 笔记已提交发布接口，当前状态为待审核。审核通过后会展示在对应频道和话题下。\n\n");
        markdown.append("## 发布位置\n\n");
        markdown.append("- 频道：").append(channel.getName()).append("\n");
        markdown.append("- 话题：").append(topic.getName()).append("\n");
        markdown.append("- 笔记类型：图文笔记\n\n");
        markdown.append("## 标题\n\n");
        markdown.append(draft.getTitle()).append("\n\n");
        if (StrUtil.isNotBlank(draft.getSummary())) {
            markdown.append("## 摘要\n\n");
            markdown.append(draft.getSummary()).append("\n\n");
        }
        markdown.append("## 正文\n\n");
        markdown.append(draft.getContent()).append("\n\n");
        if (CollUtil.isNotEmpty(imageUrls)) {
            markdown.append("## 图片\n\n");
            for (int i = 0; i < imageUrls.size(); i++) {
                markdown.append("![配图").append(i + 1).append("](").append(imageUrls.get(i)).append(")\n\n");
            }
        }
        return normalizeContent(markdown.toString());
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String buildImageWritingPlan(List<String> imageSummaries, String query) {
        UserMessage userMessage = UserMessage.from("""
                请基于图片解析结果和用户写作要求，生成一段可展示给前端用户的写作过程摘要。
                要求：
                1. 用中文输出3到5行，每行一句话；
                2. 内容只描述公开可展示的步骤，例如主题判断、素材取舍、表达角度、结构安排；
                3. 不要输出隐藏推理链，不要使用“我在思考”这类表述；
                4. 不要编造图片中无法推断的事实。

                图片解析结果：
                %s

                用户写作要求：
                %s
                """.formatted(String.join("\n", imageSummaries), query));
        ChatResponse response = qwenVisionChatModel.chat(userMessage);
        return normalizeContent(response.aiMessage().text());
    }

    private String generateImageWritingContent(List<String> imageSummaries, String writingPlan, String query) {
        UserMessage userMessage = UserMessage.from("""
                你是知识分享社区的中文识图写作助手。请根据图片解析结果和用户要求，生成一段可以直接展示给用户的正文。

                写作要求：
                1. 严格围绕图片中可见信息写作，不要编造无法判断的事实；
                2. 根据用户要求决定体裁，可以是知识分享、图文笔记、文案、说明或观察记录；
                3. 语气自然、清晰、有信息价值，不要夸张营销；
                4. 可以自然分段，但不要输出 Markdown 标题、编号列表或项目符号；
                5. 只输出最终正文，不要解释生成过程。

                图片解析结果：
                %s

                写作过程摘要：
                %s

                用户写作要求：
                %s
                """.formatted(
                String.join("\n", imageSummaries),
                StrUtil.blankToDefault(writingPlan, "暂无"),
                query
        ));
        ChatResponse response = qwenVisionChatModel.chat(userMessage);
        return response.aiMessage().text();
    }


    /**
     * ppt生成
     * @param conversationId 会话id
     * @param query 问题
     */
    public Flux<String> pptCreate(String conversationId, String query) {
        Long userId = UserContextHolder.getUserId();
        RecordingSink sink = conversationRecordService.begin(userId, conversationId, query, "pptAgent", Map.of());
        taskManager.register(sink.taskId(), sink);
        return sink.start(() -> CompletableFuture.runAsync(() -> {
        try {
            UserContextHolder.setUserId(userId);
            if (sink.isEnded()) return;


            //进行意图识别，判断式创建还是修改
            PptIntentResult intentResult = pptIntentRecognizer.recognize(conversationId, query);



            // 收集思考过程
            StringBuilder thinkingBuffer = new StringBuilder();
            // 收集最终答案
            StringBuilder finalAnswerBuffer = new StringBuilder();

            //根据意图进行路由
            switch(intentResult.pptIntent()) {
                case CREATE_PPT -> handleCreate(sink, conversationId, query, thinkingBuffer);
                case MODIFY_PPT -> handleModify(sink, conversationId, query, thinkingBuffer);
                case RESUME_PPT -> handleResume(sink, conversationId, query, thinkingBuffer);
                default -> {
                    sendEvent(sink, EventType.TYPE_THINKING, "❌ 无法识别您的意图，请重新表述\n");
                    sink.tryEmitComplete();
                }
            }
        } catch (Exception e) {
            log.error("PPT处理异常", e);
            sink.tryEmitError(e);
        } finally {
            UserContextHolder.removeThreadLocal();
        }
        }, taskExecutor));
    }

    /**
     *处理重连
     */
    private void handleResume(Sinks.Many<String> sink, String conversationId, String query, StringBuilder thinkingBuffer) {
        // 获取该会话最新的PPT实例
        AiPptInst inst = aiPptInstService.lambdaQuery()
                .eq(AiPptInst::getConversationId, conversationId)
                .eq(AiPptInst::getUserId, ((RecordingSink) sink).userId())
                .orderByDesc(AiPptInst::getCreateTime)
                .last("limit 1")
                .one();

        if (inst == null) {
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, "当前会话中没有PPT实例，请先创建一个PPT。");
            sink.tryEmitComplete();
            return;
        }

        // DB 实例无瞬态 userId，异步线程上传需要
        inst.setUserId(((RecordingSink) sink).userId());
        ((RecordingSink) sink).metadata("pptInstanceId", inst.getId());
        ((RecordingSink) sink).metadata("fileUrl", inst.getFileUrl());

        PptInstanceStatus status = inst.getStatusEnum();
        if (status == PptInstanceStatus.SUCCESS) {
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "当前PPT已成功生成，如需修改请说明具体需求。\n");
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, "当前PPT已成功生成。如果需要修改，请说明具体的修改需求。");
            sink.tryEmitComplete();
            return;
        }

        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在从状态 " + status.getDesc() + " 继续执行PPT生成...\n");
        pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
    }

    /**
     *处理修改
     */
    private void handleModify(Sinks.Many<String> sink, String conversationId, String query, StringBuilder thinkingBuffer) {
        // 获取该会话最新的PPT实例
        AiPptInst inst = aiPptInstService.lambdaQuery()
                .eq(AiPptInst::getConversationId, conversationId)
                .eq(AiPptInst::getUserId, ((RecordingSink) sink).userId())
                .orderByDesc(AiPptInst::getCreateTime)
                .last("limit 1")
                .one();

        if (inst == null) {
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, "当前会话中没有已生成的PPT，请先生成一个PPT。");
            sink.tryEmitComplete();
            return;
        }

        // DB 实例无瞬态 userId，异步线程上传需要
        inst.setUserId(((RecordingSink) sink).userId());
        ((RecordingSink) sink).metadata("pptInstanceId", inst.getId());
        ((RecordingSink) sink).metadata("fileUrl", inst.getFileUrl());

        String currentSchema = inst.getPptSchema();
        if (currentSchema == null || currentSchema.isEmpty()) {
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, "该PPT缺少Schema数据，无法修改。");
            sink.tryEmitComplete();
            return;
        }

        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在修改PPT...\n");
        
        executeModifyFlow(inst, query, sink, thinkingBuffer);
    }

    /**
     *处理创建
     */
    private void handleCreate(Sinks.Many<String> sink, String conversationId, String query, StringBuilder thinkingBuffer) {
        //创建状态机实例
        AiPptInst inst = aiPptInstService.createInst(conversationId, query);
        ((RecordingSink) sink).metadata("pptInstanceId", inst.getId());

        //启动状态机循环
        pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
    }


    private void executeModifyFlow(AiPptInst inst, String query, Sinks.Many<String> sink, StringBuilder thinkingBuffer) {
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在分析修改需求...\n");
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在修改PPT内容...\n");

        // 直接调用 SchemaStrategy 继续执行（会处理图片生成、渲染等）
        pptStatusStrategyFactory.executeSchemaStrategy(inst, sink, query, thinkingBuffer);
    }

    /**
     * 同步历史消息
     */
    private void addMessages2Memory(ChatMemory chatMemory, List<AiMessage> history) {
        for (AiMessage message : history) {
            String answer = message.getAnswer();
            String question = message.getQuestion();
            if (StrUtil.isNotBlank(question)) {
                chatMemory.add(UserMessage.from(question));
            }
            if (StrUtil.isNotBlank(answer)) {
                chatMemory.add(dev.langchain4j.data.message.AiMessage.from(answer));
            }
        }
    }

    /**
     * 获取之前的对话历史
     */
    private List<AiMessage> getHistoryMessages(Long userId, String conversationId) {
        List<AiMessage> history = aiMessageService.lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getAgentType, "webSearchAgent")
                .eq(AiMessage::getStatus, "COMPLETED")
                .eq(AiMessage::getDeleted, 0)
                .orderByDesc(AiMessage::getId)
                .last("limit " + MAX_SIZE)
                .list();
        Collections.reverse(history);
        return history;
    }

    /**
     * 构建用户隔离的 memory key，防止跨用户访问对话记忆
     */
    private String buildMemoryId(Long userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    private List<String> generateRecommendations(String query, String searchResult) {
        try {
            log.info("开始生成推荐问题: query={}", query);
            List<String> recommendations = recommendTool.recommend(query, searchResult);
            if (recommendations != null && !recommendations.isEmpty()) {
                log.info("生成推荐问题成功: {} 条", recommendations.size());
                return recommendations;
            }
        } catch (Exception e) {
            log.error("生成推荐问题失败: query={}", query, e);
        }
        return null;
    }

    /**
     * 发送 SSE 事件，返回 false 表示客户端已断开
     */
    private boolean sendEvent(Sinks.Many<String> sink, String eventType, Object content) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", eventType);
            data.put("content", content);

            String jsonData = wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil.toJsonString(data);
            String event = String.format("%s\n\n", jsonData);

            Sinks.EmitResult result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.warn("SSE 发送失败，客户端已断开: eventType={}, result={}", eventType, result);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("发送事件失败: eventType={}", eventType, e);
            return false;
        }
    }


}
    
