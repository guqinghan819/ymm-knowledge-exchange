package wcg.yhm.ai.biz.agent.pptx.strategy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.pptx.assistant.SchemaAssistant;
import wcg.yhm.ai.biz.agent.pptx.strategy.impl.*;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.service.IAiPptInstService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.ai.biz.service.RecordingSink;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: ppt状态策略工厂
 * @Version: 1.0
 */

@Component
@Slf4j
public class PptStatusStrategyFactory {
    private static final Map<PptInstanceStatus, PptStateStrategy> FACTORY =  new HashMap<>();

    @Resource
    private RequirementStrategy requirementStrategy;

    @Resource
    private SearchStrategy searchStrategy;

    @Resource
    private TemplateStrategy templateStrategy;

    @Resource
    private OutlineStrategy outlineStrategy;

    @Resource
    private SchemaStrategy schemaStrategy;

    @Resource
    private RenderStrategy renderStrategy;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    private FailedStrategy failedStrategy;

    @Resource
    private SchemaAssistant schemaAssistant;

    @PostConstruct
    public void init() {
        FACTORY.put(PptInstanceStatus.REQUIREMENT, requirementStrategy);
        FACTORY.put(PptInstanceStatus.SEARCH, searchStrategy);
        FACTORY.put(PptInstanceStatus.TEMPLATE, templateStrategy);
        FACTORY.put(PptInstanceStatus.OUTLINE, outlineStrategy);
        FACTORY.put(PptInstanceStatus.SCHEMA, schemaStrategy);
        FACTORY.put(PptInstanceStatus.RENDER, renderStrategy);
        FACTORY.put(PptInstanceStatus.FAILED, failedStrategy);
    }

    /**
     * 获取指定状态的策略
     */
    public PptStateStrategy getStrategy(PptInstanceStatus status) {
        PptStateStrategy strategy = FACTORY.get(status);
        if (strategy == null) {
            log.warn("未找到状态对应的策略: {}", status);
            return new DefaultStrategy();
        }
        return strategy;
    }

    /**
     * 默认策略，用于处理未知状态
     */
    private static class DefaultStrategy implements PptStateStrategy {
        @Override
        public void execute(AiPptInst inst, Sinks.Many<String> sink, String query,
                            StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            log.warn("未知状态: {}", inst.getStatusEnum());
            sink.tryEmitNext( String.format("{\"type\":\"%s\",\"content\":\"%s\"}",
                    "thinking", "❌ 状态异常，终止执行\n".replace("\"", "\\\"").replace("\n", "\\n")));
            sink.tryEmitComplete();
        }

        @Override
        public PptInstanceStatus getTargetStatus() {
            return PptInstanceStatus.FAILED;
        }
    }


    /**
     * 执行下一个状态
     */
    public void executeNextState(AiPptInst inst, Sinks.Many<String> sink, String query,
                                 StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        try {
            // 重新加载最新状态
            AiPptInst latestInst = aiPptInstService.getById(inst.getId());
            if (latestInst != null && java.util.Objects.equals(latestInst.getUserId(), inst.getUserId())) {
                // 保留瞬态 userId，异步线程下 DB 重载对象没有此字段
                latestInst.setUserId(inst.getUserId());
                inst = latestInst;

                // 检查是否有错误信息，如果有则说明是断点重连
                if (latestInst.getErrorMsg() != null && !latestInst.getErrorMsg().isEmpty()
                        && latestInst.getStatusEnum() != PptInstanceStatus.SUCCESS) {
                    log.info("检测到断点重连: status={}, errorMsg={}",
                            latestInst.getStatusEnum(), latestInst.getErrorMsg());
                    // 清空错误信息，允许继续执行
                    latestInst.setErrorMsg("");
                    aiPptInstService.updateById(latestInst);
                }
            }

            PptInstanceStatus status = inst.getStatusEnum();
            log.info("状态机执行: status={}", status);

            PptStateStrategy strategy = getStrategy(status);
            strategy.execute(inst, sink, query, thinkingBuffer);
        } catch (Exception e) {
            log.error("继续状态机执行失败", e);
            sink.tryEmitError(e);
        }
    }

    /**
     * 执行 FAILED 状态策略
     * 统一处理失败场景，避免各策略重复创建 FailedStrategy 实例
     */
    public void executeFailedState(AiPptInst inst, Sinks.Many<String> sink, String query,
                                   StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        PptStateStrategy failedStrategy = getStrategy(PptInstanceStatus.FAILED);
        failedStrategy.execute(inst, sink, query, thinkingBuffer);
    }

    /**
     * 执行 Schema 策略，支持修改模式
     * 用于修改流程，直接调用 SchemaStrategy 而不需要通过状态机流转
     *
     * @param inst PPT 实例
     * @param sink 输出 sink
     * @param query 用户查询
     * @param thinkingBuffer 思考缓冲
     */
    public void executeSchemaStrategy(AiPptInst inst, Sinks.Many<String> sink, String query,
                                      StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        schemaStrategy.executeWithModify(inst, sink, query, thinkingBuffer);
    }
}
    
