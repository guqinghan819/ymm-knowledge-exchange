package wcg.yhm.ai.biz.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.ai.biz.domain.pojo.AiMessage;
import wcg.yhm.ai.biz.domain.response.GetMessageResponse;
import wcg.yhm.ai.biz.domain.vo.MessageVO;
import wcg.yhm.ai.biz.enums.ErrorCodeEnum;
import wcg.yhm.ai.biz.service.IAiMessageService;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-09
 * @Description:
 * @Version: 1.0
 */

@RestController
@RequestMapping("/agent")
public class AiMessageController {

    @Resource
    private IAiMessageService aiMessageService;

    @Resource
    private wcg.yhm.ai.biz.service.IAiConversationService aiConversationService;

    @GetMapping("/message/get/{conversationId}")
    @ApiOperationLog(description = "获取对话详情")
    public Response<GetMessageResponse> getMessage(@PathVariable("conversationId") String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Long userId = wcg.yhm.framework.context.utils.UserContextHolder.getUserId();
        if (userId == null || !aiConversationService.lambdaQuery()
                .eq(wcg.yhm.ai.biz.domain.pojo.AiConversation::getUserId, userId)
                .eq(wcg.yhm.ai.biz.domain.pojo.AiConversation::getConversationId, conversationId)
                .eq(wcg.yhm.ai.biz.domain.pojo.AiConversation::getDeleted, 0).exists()) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        List<AiMessage> list = aiMessageService.lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId)
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getDeleted, 0)
                .orderByAsc(AiMessage::getCreatedTime, AiMessage::getId)
                .list();

        List<MessageVO> messageVOS = list.stream()
                .map(m -> {
                    MessageVO vo = BeanUtil.copyProperties(m, MessageVO.class, "metadata");
                    if (StrUtil.isNotBlank(m.getMetadata())) {
                        try { vo.setMetadata(com.alibaba.fastjson.JSON.parseObject(m.getMetadata())); }
                        catch (RuntimeException ignored) { vo.setMetadata(java.util.Map.of()); }
                    }
                    return vo;
                })
                .toList();

        return Response.success(new GetMessageResponse(conversationId, messageVOS));
    }
}
    
