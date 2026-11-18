package wcg.yhm.ai.biz.agent.pptx.strategy.impl;

import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.pptx.assistant.SearchAssistant;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStateStrategy;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStatusStrategyFactory;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.service.IAiPptInstService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.ai.biz.service.RecordingSink;
import wcg.yhm.framework.context.utils.UserContextHolder;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: 信息收集策略
 * @Version: 1.0
 */

@Slf4j
@Component
public class SearchStrategy implements PptStateStrategy {

    private static final PptInstanceStatus TARGET_STATUS = PptInstanceStatus.TEMPLATE;

    @Resource
    private SearchAssistant searchAssistant;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    @Lazy
    private PptStatusStrategyFactory pptStatusStrategyFactory;

    @Override
    public void execute(AiPptInst inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在收集相关信息...\n");

        Long userId = inst.getUserId();
        String memoryId = userId + ":" + inst.getConversationId();

        // 以需求澄清阶段确认的需求作为搜索主题
        String topic = inst.getRequirement();
        log.info("信息收集开始 - memoryId: {}, topic: {}", memoryId, topic);

        TokenStream tokenStream = searchAssistant.search(memoryId, topic);
        StringBuilder responseBuffer = new StringBuilder();

        // 流式输出 AI 整理后的搜索总结
        tokenStream.onPartialResponse(partialText -> {
            responseBuffer.append(partialText);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, partialText);
        });

        // 收集完成：保存结果，进入下一阶段
        tokenStream.onCompleteResponse(response -> {
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            String fullResponse = responseBuffer.toString();
            thinkingBuffer.append(fullResponse);

            inst.setSearchInfo(fullResponse);
            inst.setStatusEnum(TARGET_STATUS);
            aiPptInstService.updateById(inst);
            log.info("信息收集完成，实例 {} 状态: {} -> {}", inst.getId(), inst.getStatus(), TARGET_STATUS);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "\n信息收集完成，即将进入模板选择阶段...\n");
            pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
        });

        tokenStream.onError(error -> {
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            log.error("信息收集AI调用失败: memoryId={}", memoryId, error);
            String errorMsg = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, "信息收集失败: " + errorMsg);
            // 失败时不回退状态，只更新错误信息，转到 FAILED
            inst.setErrorMsg("信息收集失败: " + errorMsg);
            inst.setStatusEnum(PptInstanceStatus.SEARCH);
            aiPptInstService.updateById(inst);
            // TODO: 跳转到失败策略
            // pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(error);
        });

        tokenStream.start();
    }

    @Override
    public PptInstanceStatus getTargetStatus() {
        return TARGET_STATUS;
    }
}
