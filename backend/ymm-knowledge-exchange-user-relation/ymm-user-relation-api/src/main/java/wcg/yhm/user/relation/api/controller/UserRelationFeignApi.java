package wcg.yhm.user.relation.api.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import wcg.yhm.user.relation.api.constants.ServiceNameConstant;
import wcg.yhm.user.relation.api.domain.request.AdminFanRankRequest;
import wcg.yhm.user.relation.api.domain.request.QueryFollowRequest;
import wcg.yhm.user.relation.api.domain.response.FanRankResponse;
import wcg.yhm.user.relation.api.domain.response.FollowResultResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 用户关系远程调用api
 * @Version: 1.0
 */

@FeignClient(name = ServiceNameConstant.SERVICE_NAME)
public interface UserRelationFeignApi {

    String PATH_PREFIX_FOLLOW = "/following";

    String PATH_PREFIX_FANS = "/fans";

    @PostMapping(value = PATH_PREFIX_FOLLOW + "/batch/check/follow")
    Response<List<FollowResultResponse>> checkFollow(@Validated @RequestBody QueryFollowRequest queryFollowRequest);

    @PostMapping(value = PATH_PREFIX_FANS + "/admin/rank")
    Response<List<FanRankResponse>> adminFanRank(@Validated @RequestBody AdminFanRankRequest request);
}
    
