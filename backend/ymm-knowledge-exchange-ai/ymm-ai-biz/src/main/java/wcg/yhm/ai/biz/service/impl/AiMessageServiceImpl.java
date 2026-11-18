package wcg.yhm.ai.biz.service.impl;

import wcg.yhm.ai.biz.domain.pojo.AiMessage;
import wcg.yhm.ai.biz.mapper.AiMessageMapper;
import wcg.yhm.ai.biz.service.IAiMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * AI对话消息表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-08
 */
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements IAiMessageService {

}
