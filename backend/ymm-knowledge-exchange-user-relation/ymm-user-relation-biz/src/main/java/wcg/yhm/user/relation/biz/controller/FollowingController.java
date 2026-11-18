package wcg.yhm.user.relation.biz.controller;


import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.user.relation.api.domain.request.QueryFollowRequest;
import wcg.yhm.user.relation.api.domain.response.FollowResultResponse;
import wcg.yhm.user.relation.biz.service.IFollowingService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 用户关注表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2026-01-25
 */
@RestController
@RequestMapping("/following")
public class FollowingController {
    @Resource
    private IFollowingService followingService;

    @PostMapping("/batch/check/follow")
    @ApiOperationLog(description = "批量查询是否关注指定用户")
    public Response<List<FollowResultResponse>> checkFollow(@Validated @RequestBody QueryFollowRequest queryFollowRequest) {
        List<FollowResultResponse> followResultResponses = followingService.checkFollow(queryFollowRequest);
        return Response.success(followResultResponses);
    }

}
