package wcg.yhm.note.controller;


import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.note.model.vo.ChannelResponse;
import wcg.yhm.note.service.IChannelService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 频道表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/channel")
public class ChannelController {
    @Resource
    private IChannelService channelService;

    @GetMapping("/get")
    @ApiOperationLog(description = "获取所有频道")
    public Response<List<ChannelResponse>> getChannel() {
        List<ChannelResponse> channel = channelService.getChannel();
        return Response.success(channel);
    }
}
