package wcg.yhm.ai.biz.service.impl;


import lombok.extern.slf4j.Slf4j;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.mapper.AiPptInstMapper;
import wcg.yhm.ai.biz.service.IAiPptInstService;
import wcg.yhm.framework.context.utils.UserContextHolder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * AI PPT生成实例表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-11
 */
@Service
@Slf4j
public class AiPptInstServiceImpl extends ServiceImpl<AiPptInstMapper, AiPptInst> implements IAiPptInstService {

    @Override
    public AiPptInst createInst(String conversationId, String query) {
        AiPptInst inst = AiPptInst.builder()
                .conversationId(conversationId)
                .query(query)
                .status(PptInstanceStatus.REQUIREMENT.getCode())
                .userId(UserContextHolder.getUserId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        save(inst);
        log.info("创建PPT实例: id={}, conversationId={}", inst.getId(), conversationId);
        return inst;
    }
}
