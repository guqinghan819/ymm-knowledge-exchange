package wcg.yhm.user.relation.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import wcg.yhm.user.api.controller.UserFeignApi;
import wcg.yhm.user.api.domain.dto.GetUserByPhoneRequest;
import wcg.yhm.user.api.domain.dto.GetUserListByIdsRequest;
import wcg.yhm.user.api.domain.dto.QueryCreatorByIdRequest;
import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description: 用户服务远程调用
 * @Version: 1.0
 */

@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 获取用户信息
     * @param userId 目标用户
     * @return 用户信息
     */
    public CreatorResponse getUserById(Long userId) {
        QueryCreatorByIdRequest queryCreatorByIdRequest = new QueryCreatorByIdRequest();
        queryCreatorByIdRequest.setCreatorId(userId);

        Response<CreatorResponse> response = userFeignApi.getCreatorById(queryCreatorByIdRequest);
        if (!response.isSuccess() || response.getData() == null) {
            return null;
        }

        return response.getData();
    }

    /**
     * 批量查询用户信息
     * @return 信息
     */
    public List<FindUserResponse> findByIds(List<Long> ids) {
        GetUserListByIdsRequest getUserListByIdsRequest = new GetUserListByIdsRequest(ids);
        Response<List<FindUserResponse>> response = userFeignApi.batchQueryUser(getUserListByIdsRequest);
        if (!response.isSuccess() || Objects.isNull(response.getData()) || CollUtil.isEmpty(response.getData())) {
            return null;
        }
        return response.getData();
    }


}
    
