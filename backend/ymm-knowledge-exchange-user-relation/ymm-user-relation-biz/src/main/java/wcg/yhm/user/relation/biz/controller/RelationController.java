package wcg.yhm.user.relation.biz.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.user.relation.biz.domain.dto.FindFansRequest;
import wcg.yhm.user.relation.biz.domain.dto.FindFollowUserRequest;
import wcg.yhm.user.relation.biz.domain.dto.FollowingRequest;
import wcg.yhm.user.relation.biz.domain.dto.UnFollowRequest;
import wcg.yhm.user.relation.biz.domain.vo.FindFansResponse;
import wcg.yhm.user.relation.biz.domain.vo.FindFollowUserResponse;
import wcg.yhm.user.relation.biz.service.IFansService;
import wcg.yhm.user.relation.biz.service.IFollowingService;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description:
 * @Version: 1.0
 */

@RestController
@Slf4j
@RequestMapping("/relation")
public class RelationController {
    @Resource
    private IFollowingService followingService;
    @Resource
    private IFansService fansService;

    @PostMapping("/follow")
    @ApiOperationLog(description = "关注用户")
    public Response<?> follow(@RequestBody @Validated FollowingRequest followingRequest) {
        followingService.followUser(followingRequest);
        return Response.success();
    }

    @PostMapping("/unfollow")
    @ApiOperationLog(description = "取消关注")
    public Response<?> unfollow(@RequestBody @Validated UnFollowRequest unFollowRequest) {
        followingService.unfollowUser(unFollowRequest);
        return Response.success();
    }

    @PostMapping("/follow/list")
    @ApiOperationLog(description = "获取关注列表")
    public PageResponse<FindFollowUserResponse> findFollowList(@RequestBody @Validated FindFollowUserRequest findFollowUserRequest) {
        return followingService.findFollowingList(findFollowUserRequest);
    }

    @PostMapping("/fans/list")
    @ApiOperationLog(description = "获取粉丝列表")
    public PageResponse<FindFansResponse> findFansList(@RequestBody @Validated FindFansRequest findFansRequest) {
        return fansService.findFansList(findFansRequest);
    }
}
    
