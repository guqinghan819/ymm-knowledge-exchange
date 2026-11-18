package wcg.yhm.ai.biz.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import wcg.yhm.ai.biz.domain.pojo.AiConversation;
import wcg.yhm.ai.biz.domain.response.GetConversationResponse;
import wcg.yhm.ai.biz.domain.vo.ConversationVO;
import wcg.yhm.ai.biz.enums.ErrorCodeEnum;
import wcg.yhm.ai.biz.service.IAiConversationService;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-09
 * @Description: ai会话
 * @Version: 1.0
 */

@RestController
@RequestMapping("/agent")
public class AiConversationController {

    @Resource
    private IAiConversationService aiConversationService;

    @ApiOperationLog(description = "获取用户的15条会话记录")
    @GetMapping("/conversation/get")
    public Response<GetConversationResponse> createConversation() {
        Long userId = UserContextHolder.getUserId();
        if (Objects.isNull(userId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        List<AiConversation> list = aiConversationService.lambdaQuery()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getDeleted, 0)
                .orderByDesc(AiConversation::getUpdatedTime, AiConversation::getId)
                .last("limit 15")
                .list();
        List<ConversationVO> vos = list.stream().map(c -> {
            return ConversationVO.builder()
                    .conversationId(c.getConversationId())
                    .title(c.getTitle())
                    .agentType(c.getAgentType())
                    .updatedTime(c.getUpdatedTime())
                    .createdTime(c.getCreatedTime())
                    .build();
        }).toList();
        return Response.success(new GetConversationResponse(vos));
    }

    @ApiOperationLog(description = "删除当前用户全部会话")
    @DeleteMapping("/conversation/delete")
    public Response<Void> deleteAll() {
        Long userId = UserContextHolder.getUserId();
        if (Objects.isNull(userId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        aiConversationService.deleteAll(userId);
        return Response.success();
    }

    @ApiOperationLog(description = "根据会话ID删除指定会话")
    @DeleteMapping("/conversation/delete/{conversationId}")
    public Response<Void> deleteByConversationId(@PathVariable String conversationId) {
        Long userId = UserContextHolder.getUserId();
        if (Objects.isNull(userId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        aiConversationService.deleteByConversationId(userId, conversationId);
        return Response.success();
    }


}
    
