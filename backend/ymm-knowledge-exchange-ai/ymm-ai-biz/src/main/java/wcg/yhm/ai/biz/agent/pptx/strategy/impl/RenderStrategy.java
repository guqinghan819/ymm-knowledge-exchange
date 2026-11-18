package wcg.yhm.ai.biz.agent.pptx.strategy.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStateStrategy;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStatusStrategyFactory;
import wcg.yhm.ai.biz.agent.service.PptPythonRenderService;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.service.IAiPptInstService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.ai.biz.service.RecordingSink;
import wcg.yhm.framework.context.utils.UserContextHolder;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: ppt渲染策略
 * @Version: 1.0
 */

@Slf4j
@Component
public class RenderStrategy implements PptStateStrategy {

    private static final PptInstanceStatus TARGET_STATUS = PptInstanceStatus.SUCCESS;

    @Resource
    private PptPythonRenderService pptPythonRenderService;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    @Lazy
    private PptStatusStrategyFactory pptStatusStrategyFactory;

    @Override
    public void execute(AiPptInst inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在渲染PPT...\n");

        String pptSchemaJson = inst.getPptSchema();
        log.info("PPT渲染开始 - instId: {}", inst.getId());

        try {
            // 异步线程上设入 userId 供 Feign OSS 上传透传
            UserContextHolder.setUserId(inst.getUserId());

            String fileUrl = pptPythonRenderService.renderPpt(inst, pptSchemaJson);

            if (sink instanceof RecordingSink recording) {
                if (recording.isEnded()) return;
                recording.metadata("fileUrl", fileUrl);
                recording.metadata("pptInstanceId", inst.getId());
            }
            inst.setFileUrl(fileUrl);
            inst.setStatusEnum(TARGET_STATUS);
            aiPptInstService.updateById(inst);
            log.info("PPT渲染完成，实例 {} 状态: {} -> {}, fileUrl: {}", inst.getId(), inst.getStatus(), TARGET_STATUS, fileUrl);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "✅ PPT渲染完成\n");
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, "PPT已生成，点击下载：" + fileUrl);
            sink.tryEmitComplete();
        } catch (Exception e) {
            log.error("PPT渲染异常: instId={}", inst.getId(), e);
            // 失败时不回退状态，只更新错误信息，转到 FAILED
            String errorMsg = "PPT渲染失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, errorMsg);
            inst.setErrorMsg(errorMsg);
            inst.setStatusEnum(PptInstanceStatus.RENDER);
            aiPptInstService.updateById(inst);
            // TODO: 跳转到失败策略
            // pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(e);
        } finally {
            UserContextHolder.removeThreadLocal();
        }
    }

    @Override
    public PptInstanceStatus getTargetStatus() {
        return TARGET_STATUS;
    }
}
    
