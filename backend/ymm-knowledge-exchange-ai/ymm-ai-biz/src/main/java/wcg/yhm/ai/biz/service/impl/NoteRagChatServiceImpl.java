package wcg.yhm.ai.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.tool.RecommendTool;
import wcg.yhm.ai.biz.agent.tool.TitleGenerator;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.model.NoteRagContext;
import wcg.yhm.ai.biz.domain.model.NoteRagHistoryTurn;
import wcg.yhm.ai.biz.domain.model.NoteRagRetrieveResult;
import wcg.yhm.ai.biz.domain.pojo.AiConversation;
import wcg.yhm.ai.biz.domain.pojo.AiMessage;
import wcg.yhm.ai.biz.domain.request.NoteRagChatRequest;
import wcg.yhm.ai.biz.domain.vo.NoteRagReferenceVO;
import wcg.yhm.ai.biz.manager.TaskManager;
import wcg.yhm.ai.biz.service.IAiConversationService;
import wcg.yhm.ai.biz.service.IAiMessageService;
import wcg.yhm.ai.biz.service.AiConversationRecordService;
import wcg.yhm.ai.biz.service.RecordingSink;
import wcg.yhm.ai.biz.service.INoteRagChatService;
import wcg.yhm.ai.biz.service.INoteRagRetrieveService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 笔记 RAG 流式问答实现。
 */
@Slf4j
@Service
public class NoteRagChatServiceImpl implements INoteRagChatService {

    private static final String AGENT_TYPE = "noteRagAgent";
    private static final String NO_RESULT_MESSAGE = "平台暂无相关笔记资料，你可以换个更具体的问法再试试。";
    private static final String RELATED_RESULT_PREFIX = "已找到相关笔记，但暂时无法生成可靠回答：";

    @Resource
    private INoteRagRetrieveService noteRagRetrieveService;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private IAiMessageService aiMessageService;

    @Resource
    private IAiConversationService aiConversationService;

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

    @Override
    public Flux<String> chatStream(NoteRagChatRequest request) {
        // 先建立可持久化的 SSE 流，再将检索和模型生成交给线程池，避免请求线程等待。
        Long userId = UserContextHolder.getUserId();
        String query = request.getQuery().trim();
        String conversationId = request.getConversationId().trim();
        RecordingSink sink = conversationRecordService.begin(userId, conversationId, query, AGENT_TYPE, java.util.Map.of());
        String taskKey = sink.taskId();
        taskManager.register(taskKey, sink);
        return sink.start(() -> CompletableFuture.runAsync(
                () -> executeChat(taskKey, conversationId, query, false, sink), taskExecutor));
    }

    private void executeChat(String taskKey,
                             String conversationId,
                             String query,
                             boolean isNewConversation,
                             Sinks.Many<String> sink) {
        try {
            if (taskManager.isCancelled(taskKey)) {
                sink.tryEmitComplete();
                return;
            }

            SseUtil.sendEvent(sink, EventType.TYPE_STEP, "正在检索平台笔记资料...");
            List<NoteRagHistoryTurn> history = isNewConversation ? List.of() : loadHistory(((RecordingSink) sink).userId(), conversationId);
            NoteRagRetrieveResult retrieveResult = noteRagRetrieveService.retrieve(query, history);
            if (((RecordingSink) sink).isEnded()) return;
            List<NoteRagReferenceVO> references = toReferences(retrieveResult);
            // 没有候选笔记时直接提示；有相关笔记但证据不足时只返回参考资料。
            if (retrieveResult == null || CollUtil.isEmpty(retrieveResult.getCandidates())) {
                handleNoResult(taskKey, conversationId, query, sink, isNewConversation);
                return;
            }

            if (!retrieveResult.isAnswerable() || CollUtil.isEmpty(retrieveResult.getContexts())) {
                handleRelatedResult(taskKey, conversationId, query, retrieveResult, references, sink,
                        isNewConversation);
                return;
            }

            SseUtil.sendEvent(sink, EventType.TYPE_REFERENCE, references);
            SseUtil.sendEvent(sink, EventType.TYPE_STEP, "已找到相关笔记，正在生成回答...");
            String prompt = buildAnswerPrompt(retrieveResult.getStandaloneQuery(), retrieveResult.getContexts());
            StringBuilder finalAnswerBuffer = new StringBuilder();

            streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    emitText(taskKey, sink, finalAnswerBuffer, partialResponse);
                }

                @Override
                public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                    taskManager.bindHandle(taskKey, context.streamingHandle());
                    emitText(taskKey, sink, finalAnswerBuffer, partialResponse.text());
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    handleComplete(taskKey, conversationId, query, finalAnswerBuffer.toString(),
                            references, sink, isNewConversation);
                }

                @Override
                public void onError(Throwable error) {
                    handleError(taskKey, query, error, sink);
                }
            });
        } catch (Exception e) {
            handleError(taskKey, query, e, sink);
        }
    }

    private void emitText(String taskKey,
                          Sinks.Many<String> sink,
                          StringBuilder finalAnswerBuffer,
                          String partialText) {
        if (taskManager.isCancelled(taskKey) || partialText == null || partialText.isEmpty()) {
            return;
        }
        if (!SseUtil.sendEvent(sink, EventType.TYPE_TEXT, partialText)) {
            taskManager.cancel(taskKey);
            return;
        }
        finalAnswerBuffer.append(partialText);
    }

    private void handleComplete(String taskKey,
                                String conversationId,
                                String query,
                                String answer,
                                List<NoteRagReferenceVO> references,
                                Sinks.Many<String> sink,
                                boolean isNewConversation) {
        try {
            if (taskManager.isCancelled(taskKey)) {
                sink.tryEmitComplete();
                return;
            }
            String referenceJson = JsonUtil.toJsonString(references);
            List<String> recommendations = generateRecommendations(query, referenceJson);
            if (CollUtil.isNotEmpty(recommendations)) {
                SseUtil.sendEvent(sink, EventType.TYPE_RECOMMEND, recommendations);
            }
            SseUtil.sendEvent(sink, "complete", "done");
        } catch (Exception error) {
            sink.tryEmitError(error);
        } finally {
            sink.tryEmitComplete();
        }
    }

    private void handleNoResult(String taskKey,
                                String conversationId,
                                String query,
                                Sinks.Many<String> sink,
                                boolean isNewConversation) {
        try {
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, NO_RESULT_MESSAGE);
            SseUtil.sendEvent(sink, "complete", "done");
        } finally {
            sink.tryEmitComplete();
        }
    }

    private void handleRelatedResult(String taskKey,
                                     String conversationId,
                                     String query,
                                     NoteRagRetrieveResult retrieveResult,
                                     List<NoteRagReferenceVO> references,
                                     Sinks.Many<String> sink,
                                     boolean isNewConversation) {
        try {
            if (CollUtil.isNotEmpty(references)) {
                SseUtil.sendEvent(sink, EventType.TYPE_REFERENCE, references);
            }
            String reason = StrUtil.blankToDefault(retrieveResult.getEvidenceReason(), "当前问题需要更明确的提问意图");
            String message = RELATED_RESULT_PREFIX + reason + "。你可以查看下方来源，或换个更具体的问法。";
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, message);
            SseUtil.sendEvent(sink, "complete", "done");
        } finally {
            sink.tryEmitComplete();
        }
    }

    private void handleError(String taskKey, String query, Throwable error, Sinks.Many<String> sink) {
        log.error("笔记RAG问答失败: query={}", query, error);
        String errorMessage = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        SseUtil.sendEvent(sink, EventType.TYPE_ERROR, errorMessage);
        sink.tryEmitComplete();
    }

    private String buildAnswerPrompt(String query, List<NoteRagContext> contexts) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                你是雨绵绵知识分享平台的笔记问答助手。请严格基于【平台笔记资料】回答用户问题。

                回答规则：
                1. 只能使用平台笔记资料中的信息，不要编造资料外的事实；
                2. 关键事实、结论和建议后必须标注对应行内引用编号，例如 [1] 或 [1][2]；
                3. 多篇笔记说法冲突时分别陈述并分别引用，不自行裁决；
                4. 不得使用常识或模型知识补齐；回答要自然、清晰；
                5. 引用编号只能使用下方已经分配的编号。

                用户问题：
                """).append(query).append("\n\n平台笔记资料：\n");

        for (int i = 0; i < contexts.size(); i++) {
            NoteRagContext context = contexts.get(i);
            NoteRagReferenceVO reference = context.getReference();
            int citationNo = reference.getCitationNo() == null ? i + 1 : reference.getCitationNo();
            prompt.append("\n[").append(citationNo).append("]\n")
                    .append("标题：").append(StrUtil.blankToDefault(reference.getTitle(), "未命名笔记")).append("\n")
                    .append("话题：").append(StrUtil.blankToDefault(reference.getTopicName(), "无")).append("\n")
                    .append("作者：").append(StrUtil.blankToDefault(reference.getCreatorName(), "未知")).append("\n")
                    .append("内容：").append(context.getParentText()).append("\n");
        }
        return prompt.toString();
    }

    private List<NoteRagReferenceVO> toReferences(NoteRagRetrieveResult retrieveResult) {
        if (retrieveResult == null || CollUtil.isEmpty(retrieveResult.getReferences())) {
            return List.of();
        }
        return retrieveResult.getReferences();
    }

    private List<String> generateRecommendations(String query, String referenceJson) {
        try {
            List<String> recommendations = recommendTool.recommend(query, referenceJson);
            if (CollUtil.isNotEmpty(recommendations)) {
                return recommendations;
            }
        } catch (Exception e) {
            log.warn("笔记RAG推荐问题生成失败: query={}", query, e);
        }
        return List.of();
    }

    private List<NoteRagHistoryTurn> loadHistory(Long userId, String conversationId) {
        List<AiMessage> messages = aiMessageService.lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getStatus, "COMPLETED")
                .eq(AiMessage::getAgentType, AGENT_TYPE)
                .eq(AiMessage::getDeleted, 0)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 3")
                .list();
        List<NoteRagHistoryTurn> history = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiMessage message = messages.get(i);
            history.add(new NoteRagHistoryTurn(message.getQuestion(), message.getAnswer()));
        }
        return history;
    }

}
