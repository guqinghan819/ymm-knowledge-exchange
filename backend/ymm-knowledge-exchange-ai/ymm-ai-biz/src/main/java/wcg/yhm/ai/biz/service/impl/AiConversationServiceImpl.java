package wcg.yhm.ai.biz.service.impl;

import wcg.yhm.ai.biz.domain.pojo.AiConversation;
import wcg.yhm.ai.biz.enums.ErrorCodeEnum;
import wcg.yhm.ai.biz.mapper.AiConversationMapper;
import wcg.yhm.ai.biz.service.IAiConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.ai.biz.domain.pojo.AiMessage;
import wcg.yhm.ai.biz.service.IAiMessageService;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <p>
 * AI对话会话表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-08
 */
@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation> implements IAiConversationService {

    @jakarta.annotation.Resource
    private IAiMessageService aiMessageService;

    @jakarta.annotation.Resource
    private wcg.yhm.ai.biz.manager.TaskManager taskManager;

    @Override
    public void create(String conversationId) {
        Long userId = UserContextHolder.getUserId();
        if (Objects.isNull(userId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        AiConversation aiConversation = AiConversation.builder()
                .conversationId(conversationId)
                .userId(userId)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        this.save(aiConversation);
    }

    @Override
    @Transactional
    public void deleteAll(Long userId) {
        if (Objects.isNull(userId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        taskManager.cancelUser(userId);
        aiMessageService.lambdaUpdate().eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getDeleted, 0).set(AiMessage::getDeleted, 1).update();
        lambdaUpdate()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getDeleted, 0)
                .set(AiConversation::getDeleted, 1)
                .update();
    }

    @Override
    @Transactional
    public void deleteByConversationId(Long userId, String conversationId) {
        if (Objects.isNull(userId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        boolean exists = lambdaQuery()
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getDeleted, 0)
                .exists();
        if (!exists) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        taskManager.cancel(userId + ":" + conversationId);
        lambdaUpdate()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getDeleted, 0)
                .set(AiConversation::getDeleted, 1)
                .update();
        aiMessageService.lambdaUpdate()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getDeleted, 0)
                .set(AiMessage::getDeleted, 1)
                .update();
    }
}
