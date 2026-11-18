package wcg.yhm.note.controller;


import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.note.model.dto.AdminIdRequest;
import wcg.yhm.note.model.dto.AdminNameRequest;
import wcg.yhm.note.model.dto.AdminUpdateRequest;
import wcg.yhm.note.model.vo.TopicAdminResponse;
import wcg.yhm.note.service.ITopicService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 管理端：话题(标签)管理接口
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/topic/admin")
public class TopicAdminController {

    @Resource
    private ITopicService topicService;

    /**
     * 管理端获取话题列表(含所属频道)
     */
    @GetMapping("/list")
    @ApiOperationLog(description = "管理端获取话题列表")
    public Response<List<TopicAdminResponse>> list() {
        return Response.success(topicService.adminTopicList());
    }

    /**
     * 管理端新增话题
     */
    @PostMapping("/create")
    @ApiOperationLog(description = "管理端新增话题")
    public Response<?> create(@Validated @RequestBody AdminNameRequest request) {
        topicService.createTopic(request.getName());
        return Response.success();
    }

    /**
     * 管理端修改话题
     */
    @PostMapping("/update")
    @ApiOperationLog(description = "管理端修改话题")
    public Response<?> update(@Validated @RequestBody AdminUpdateRequest request) {
        topicService.updateTopic(request.getId(), request.getName());
        return Response.success();
    }

    /**
     * 管理端删除话题
     */
    @PostMapping("/delete")
    @ApiOperationLog(description = "管理端删除话题")
    public Response<?> delete(@Validated @RequestBody AdminIdRequest request) {
        topicService.deleteTopic(request.getId());
        return Response.success();
    }
}
