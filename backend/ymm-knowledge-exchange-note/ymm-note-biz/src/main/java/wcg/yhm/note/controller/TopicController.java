package wcg.yhm.note.controller;


import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.note.model.dto.QueryTopicRequest;
import wcg.yhm.note.model.vo.TopicResponse;
import wcg.yhm.note.service.ITopicService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 话题表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/topic")
public class TopicController {
    @Resource
    private ITopicService topicService;

    @PostMapping("/get")
    @ApiOperationLog(description = "根据频道id查询话题")
    public Response<List<TopicResponse>> getTopicByChannelId(@Validated @RequestBody QueryTopicRequest queryTopicRequest) {
        List<TopicResponse> topicByChannelId = topicService.getTopicByChannelId(queryTopicRequest);
        return Response.success(topicByChannelId);
    }
}
