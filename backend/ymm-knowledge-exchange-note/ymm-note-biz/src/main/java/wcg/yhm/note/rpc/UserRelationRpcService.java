package wcg.yhm.note.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import wcg.yhm.user.relation.api.controller.UserRelationFeignApi;
import wcg.yhm.user.relation.api.domain.request.AdminFanRankRequest;
import wcg.yhm.user.relation.api.domain.request.QueryFollowRequest;
import wcg.yhm.user.relation.api.domain.response.FanRankResponse;
import wcg.yhm.user.relation.api.domain.response.FollowResultResponse;
import wcg.yhm.note.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 用户关系服务远程调用
 * @Version: 1.0
 */

@Component
public class UserRelationRpcService {
    @Resource
    private UserRelationFeignApi userRelationFeignApi;

    /**
     * 查询是否关注指定用户
     * @param userId 登陆用户id
     * @param creatorIds 创作者id集合
     * @return 结果
     */
    public List<FollowResultResponse> checkFollow(Long userId, List<Long> creatorIds) {
        QueryFollowRequest queryFollowRequest = new QueryFollowRequest();
        queryFollowRequest.setLoginUserId(userId);
        queryFollowRequest.setCreatorId(creatorIds);
        Response<List<FollowResultResponse>> listResponse = userRelationFeignApi.checkFollow(queryFollowRequest);
        if (listResponse == null || !listResponse.isSuccess()) {
            return null;
        }
        return listResponse.getData();
    }

    /**
     * 查询指定时间范围内仍有效的新增粉丝排名。
     */
    public List<FanRankResponse> adminFanRank(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        AdminFanRankRequest request = new AdminFanRankRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setLimit(limit);
        Response<List<FanRankResponse>> response = userRelationFeignApi.adminFanRank(request);
        if (response == null || !response.isSuccess()) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        return response.getData() == null ? List.of() : response.getData();
    }
}
    
