package wcg.yhm.ai.biz.agent.pptx.strategy.impl;

import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.pptx.assistant.ErrorAssistant;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStateStrategy;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.ai.biz.service.RecordingSink;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-12
 * @Description: 失败策略
 * @Version: 1.0
 */

@Slf4j
@Component
public class FailedStrategy implements PptStateStrategy {

    @Resource
    private ErrorAssistant errorAssistant;

    private static final PptInstanceStatus TARGET_STATUS = PptInstanceStatus.FAILED;

    @Override
    public void execute(AiPptInst inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        log.info("执行失败状态策略: status={}, errorMsg={}", inst.getStatusEnum(), inst.getErrorMsg());

        String errorMsg = inst.getErrorMsg();

        // 构建思考过程
        String thinkingProcess;
        if (StringUtils.hasText(errorMsg)) {
            if (thinkingBuffer != null && !thinkingBuffer.isEmpty()) {
                thinkingProcess = String.format("""
                        之前已完成的步骤：
                        %s

                        当前遇到的错误：
                        %s
                        """, thinkingBuffer, errorMsg);
            } else {
                thinkingProcess = "PPT生成过程中遇到错误：" + errorMsg;
            }
        } else {
            thinkingProcess = "PPT生成过程中遇到未知错误，请重试";
        }

        TokenStream tokenStream = errorAssistant.error(thinkingProcess);
        StringBuilder responseBuffer = new StringBuilder();

        tokenStream.onPartialResponse(partialText -> {
            responseBuffer.append(partialText);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, partialText);
        });

        tokenStream.onCompleteResponse(response -> {
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            log.info("失败说明输出完成: {}", responseBuffer);
            thinkingBuffer.append(responseBuffer);
            sink.tryEmitComplete();
        });

        tokenStream.onError(err -> {
            log.error("输出失败说明异常", err);
            String fallbackMsg = StringUtils.hasText(errorMsg) ? errorMsg : "PPT生成失败，请重试";
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_TEXT, fallbackMsg);
            thinkingBuffer.append(fallbackMsg);
            sink.tryEmitComplete();
        });

        tokenStream.start();
    }

    @Override
    public PptInstanceStatus getTargetStatus() {
        return TARGET_STATUS;
    }
}
    
