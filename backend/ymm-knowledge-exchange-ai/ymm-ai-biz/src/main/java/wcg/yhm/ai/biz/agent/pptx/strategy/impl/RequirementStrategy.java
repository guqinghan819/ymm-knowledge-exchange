package wcg.yhm.ai.biz.agent.pptx.strategy.impl;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.enums.pptx.PptIntent;
import wcg.yhm.ai.biz.agent.pptx.assistant.RequirementAssistant;
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
 * @Description: 确认澄清策略
 * @Version: 1.0
 */

@Slf4j
@Component
public class RequirementStrategy implements PptStateStrategy {

    @Resource
    private RequirementAssistant requirementAssistant;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    @Lazy
    private PptStatusStrategyFactory pptStatusStrategyFactory;

    /**
     * 下个目标状态
     */
    private static final PptInstanceStatus TARGET_STATUS = PptInstanceStatus.SEARCH;


    @Override
    public void execute(AiPptInst inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在分析您的需求...\n");

        Long userId = inst.getUserId();
        String memoryId = userId + ":" + inst.getConversationId();

        TokenStream tokenStream = requirementAssistant.requirement(memoryId, query);
        StringBuilder responseBuffer = new StringBuilder();

        // 流式输出 AI 的需求分析/追问
        tokenStream.onPartialResponse(partialText -> {
            responseBuffer.append(partialText);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, partialText);
        });

        // 澄清完成：判断信息是否足够
        tokenStream.onCompleteResponse(response -> {
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            String fullResponse = responseBuffer.toString();
            thinkingBuffer.append(fullResponse);

            if (shouldContinueToNextStep(fullResponse)) {
                inst.setRequirement(fullResponse);
                inst.setStatusEnum(TARGET_STATUS);
                aiPptInstService.updateById(inst);
                log.info("需求澄清完成，实例 {} 状态: {} -> {}", inst.getId(), inst.getStatus(), TARGET_STATUS);
                if (sink instanceof RecordingSink recording && recording.isEnded()) return;
                SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "\n需求已确认，即将进入信息收集阶段...\n");
                pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
            } else {
                // 信息不足，保持 REQUIREMENT 状态，等待用户下一轮补充
                inst.setRequirement(fullResponse);
                inst.setErrorMsg("需求信息不足，保持澄清状态，等待用户补充");
                inst.setStatusEnum(PptInstanceStatus.REQUIREMENT);
                aiPptInstService.updateById(inst);
                log.info("需求信息不足，保持澄清状态，等待用户补充");
                // 跳转到失败策略，结束当前流
                pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            }
        });

        tokenStream.onError(error -> {
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            log.error("需求澄清AI调用失败: memoryId={}", memoryId, error);
            String errorMsg = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, "需求分析失败: " + errorMsg);
            inst.setErrorMsg("需求澄清失败: " + errorMsg);
            inst.setStatusEnum(PptInstanceStatus.REQUIREMENT);
            //跳转到失败策略
            pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(error);
        });
        tokenStream.start(); //启动流式请求
    }

    /**
     *判断是否进入下一步
     */
    private boolean shouldContinueToNextStep(String fullResponse) {
        if (StrUtil.isBlank(fullResponse)) {
            return false; //不进行下一步
        }
        //去掉左右空格
        String trimmedResponse = fullResponse.trim();
        // 优先检查明确的标记（使用精确匹配避免误判）
        if (trimmedResponse.contains("【开始生成PPT】") || trimmedResponse.contains("【开始生成PPT】".toLowerCase())) {
            return true;
        }
        if (trimmedResponse.contains("【暂停生成PPT】") || trimmedResponse.contains("【暂停生成PPT】".toLowerCase())) {
            return false;
        }
        // 兜容逻辑：如果没有找到明确标记，根据内容特征判断
        // 如果包含明确的疑问标记（问号、请问等），则不能继续
        String[] stopKeywords = {
                "【暂停生成PPT】", "【暂停生成ppt】",
                "请问", "请问您", "请问是否", "请提供", "请问需要",
                "请问想", "请问希望", "请问要", "请问您的"
        };
        String lowerResponse = trimmedResponse.toLowerCase();
        for (String keyword : stopKeywords) {
            if (lowerResponse.contains(keyword.toLowerCase())) {
                return false;
            }
        }
        // 默认可以继续
        return true;
    }

    @Override
    public PptInstanceStatus getTargetStatus() {
        return TARGET_STATUS;
    }


}
