package wcg.yhm.ai.biz.agent.pptx.strategy.impl;

import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.pptx.assistant.OutLineAssistant;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStateStrategy;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStatusStrategyFactory;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.domain.pojo.AiPptTemplate;
import wcg.yhm.ai.biz.service.IAiPptInstService;
import wcg.yhm.ai.biz.service.IAiPptTemplateService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.ai.biz.service.RecordingSink;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: 大纲生成策略
 * @Version: 1.0
 */

@Component
@Slf4j
public class OutlineStrategy implements PptStateStrategy {

    private static final PptInstanceStatus TARGET_STATUS = PptInstanceStatus.SCHEMA;

    @Resource
    private IAiPptTemplateService aiPptTemplateService;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    private OutLineAssistant outLineAssistant;

    @Resource
    @Lazy
    private PptStatusStrategyFactory pptStatusStrategyFactory;

    @Override
    public void execute(AiPptInst inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在生成PPT大纲...\n");
        String requirement = inst.getRequirement();
        String searchInfo = inst.getSearchInfo();
        String templateCode = inst.getTemplateCode();
        AiPptTemplate template = aiPptTemplateService.getByCode(templateCode);

        if (template == null) {
            log.error("模板不存在: templateCode={}", templateCode);
            //todo 失败时不回退状态，只更新错误信息，转到 FAILED
            String errorMsg = "模板不存在: " + templateCode;
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, errorMsg);
            inst.setErrorMsg(errorMsg);
            inst.setStatusEnum(PptInstanceStatus.OUTLINE);
            aiPptInstService.updateById(inst);
            // TODO: 跳转到失败策略
            // pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(new RuntimeException(errorMsg));
            return;
        }

        String templateSchema = template.getTemplateSchema();
        String templateName = template.getTemplateName();
        log.info("大纲生成开始 - instId: {}, template: {}", inst.getId(), templateName);

        TokenStream tokenStream = outLineAssistant.create(requirement, searchInfo, templateName, templateSchema);
        StringBuilder outlineContent = new StringBuilder();

        tokenStream.onPartialResponse(partialText -> {
            outlineContent.append(partialText);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, partialText);
        });

        tokenStream.onCompleteResponse(response -> {
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            String fullResponse = outlineContent.toString();
            thinkingBuffer.append(fullResponse);

            inst.setOutline(fullResponse);
            inst.setStatusEnum(TARGET_STATUS);
            aiPptInstService.updateById(inst);
            log.info("大纲生成完成，实例 {} 状态: {} -> {}", inst.getId(), inst.getStatus(), TARGET_STATUS);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "\n✅ 大纲生成完成，即将进入Schema生成阶段...\n");
            pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
        });

        tokenStream.onError(error -> {
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            log.error("大纲生成AI调用失败: instId={}", inst.getId(), error);
            String errorMsg = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, "大纲生成失败: " + errorMsg);
            // 失败时不回退状态，只更新错误信息，转到 FAILED
            inst.setErrorMsg("大纲生成失败: " + errorMsg);
            inst.setStatusEnum(PptInstanceStatus.OUTLINE);
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
    
