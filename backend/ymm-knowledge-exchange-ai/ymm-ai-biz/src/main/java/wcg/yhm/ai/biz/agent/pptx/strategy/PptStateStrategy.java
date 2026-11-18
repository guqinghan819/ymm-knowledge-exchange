package wcg.yhm.ai.biz.agent.pptx.strategy;

import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: ppt状态策略
 * @Version: 1.0
 */

public interface PptStateStrategy {
    /**
     * 执行该状态的处理逻辑
     *
     * @param inst            PPT实例
     * @param sink            响应流
     * @param query           用户查询
     * @param thinkingBuffer  思考缓冲区
     */
    void execute(AiPptInst inst, Sinks.Many<String> sink, String query,
                 StringBuilder thinkingBuffer);

    /**
     * 获取该策略对应的目标状态
     * 执行成功后，状态应该变为这个状态
     *
     * @return 目标状态
     */
    PptInstanceStatus getTargetStatus();
}
    
