package wcg.yhm.ai.biz.agent.pptx.strategy.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.pptx.assistant.TemplateAssistant;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStateStrategy;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStatusStrategyFactory;
import wcg.yhm.ai.biz.agent.result.TemplateSelectionResult;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.domain.pojo.AiPptTemplate;
import wcg.yhm.ai.biz.service.IAiPptInstService;
import wcg.yhm.ai.biz.service.IAiPptTemplateService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.ai.biz.service.RecordingSink;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: 模板选择策略
 * @Version: 1.0
 */

@Component
@Slf4j
public class TemplateStrategy implements PptStateStrategy {

    private static final PptInstanceStatus TARGET_STATUS = PptInstanceStatus.OUTLINE;

    @Resource
    private TemplateAssistant templateAssistant;

    @Resource
    private IAiPptTemplateService aiPptTemplateService;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    @Lazy
    private PptStatusStrategyFactory pptStatusStrategyFactory;

    @Override
    public void execute(AiPptInst inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在为您选择合适的PPT模板...\n");

        String requirement = inst.getRequirement();

        // 获取所有可用模板
        List<AiPptTemplate> templates = aiPptTemplateService.list();
        if (templates == null || templates.isEmpty()) {
            log.error("无可用模板");
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, "无可用模板");
            inst.setErrorMsg("无可用模板");
            inst.setStatusEnum(PptInstanceStatus.TEMPLATE);
            aiPptInstService.updateById(inst);
            // TODO: 跳转到失败策略
            // pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(new RuntimeException("无可用模板"));
            return;
        }
        // 构建模板信息字符串
        StringBuilder templatesInfo = new StringBuilder();
        for (AiPptTemplate template : templates) {
            templatesInfo.append(String.format("""
                            --------------------------------
                            template_code: %s
                            模板名称: %s
                            适用风格: %s
                            模板页数: %d
                            模板说明: %s
                            """,
                    template.getTemplateCode(),
                    template.getTemplateName(),
                    template.getStyleTags(),
                    template.getSlideCount(),
                    template.getTemplateDesc()
            ));
        }

        try {
            TemplateSelectionResult result = templateAssistant.select(requirement,templatesInfo.toString());

            log.info("模板选择结果: templateCode={}, reason={}", result.templateCode(), result.reason());

            inst.setTemplateCode(result.templateCode());
            inst.setStatusEnum(TARGET_STATUS);
            aiPptInstService.updateById(inst);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "✅ 模板选择完成，即将生成大纲\n");
            pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
        } catch (Exception e) {
            log.error("模板选择异常", e);
            // 失败时不回退状态，只更新错误信息，转到 FAILED
            String errorMsg = "模板选择失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, errorMsg);
            inst.setErrorMsg(errorMsg);
            inst.setStatusEnum(PptInstanceStatus.TEMPLATE);
            aiPptInstService.updateById(inst);
            // TODO: 跳转到失败策略
            // pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(e);
        }
    }

    @Override
    public PptInstanceStatus getTargetStatus() {
        return TARGET_STATUS;
    }
}
    
