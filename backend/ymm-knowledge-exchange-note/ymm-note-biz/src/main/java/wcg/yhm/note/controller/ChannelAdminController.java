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
import wcg.yhm.note.model.dto.BindTopicsRequest;
import wcg.yhm.note.model.vo.ChannelAdminResponse;
import wcg.yhm.note.service.IChannelService;
import wcg.yhm.note.service.IChannelTopicRelService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 管理端：频道(分类)管理接口
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/channel/admin")
public class ChannelAdminController {

    @Resource
    private IChannelService channelService;

    @Resource
    private IChannelTopicRelService channelTopicRelService;

    /**
     * 管理端获取频道列表(含绑定话题)
     */
    @GetMapping("/list")
    @ApiOperationLog(description = "管理端获取频道列表")
    public Response<List<ChannelAdminResponse>> list() {
        return Response.success(channelService.adminChannelList());
    }

    /**
     * 管理端新增频道
     */
    @PostMapping("/create")
    @ApiOperationLog(description = "管理端新增频道")
    public Response<?> create(@Validated @RequestBody AdminNameRequest request) {
        channelService.createChannel(request.getName());
        return Response.success();
    }

    /**
     * 管理端修改频道
     */
    @PostMapping("/update")
    @ApiOperationLog(description = "管理端修改频道")
    public Response<?> update(@Validated @RequestBody AdminUpdateRequest request) {
        channelService.updateChannel(request.getId(), request.getName());
        return Response.success();
    }

    /**
     * 管理端删除频道
     */
    @PostMapping("/delete")
    @ApiOperationLog(description = "管理端删除频道")
    public Response<?> delete(@Validated @RequestBody AdminIdRequest request) {
        channelService.deleteChannel(request.getId());
        return Response.success();
    }

    /**
     * 管理端绑定频道话题(覆盖式)
     */
    @PostMapping("/bindTopics")
    @ApiOperationLog(description = "管理端绑定频道话题")
    public Response<?> bindTopics(@Validated @RequestBody BindTopicsRequest request) {
        channelTopicRelService.bindTopics(request.getChannelId(), request.getTopicIds());
        return Response.success();
    }
}
