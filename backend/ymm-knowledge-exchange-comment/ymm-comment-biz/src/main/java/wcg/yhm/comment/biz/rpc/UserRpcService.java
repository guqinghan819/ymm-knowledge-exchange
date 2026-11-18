package wcg.yhm.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.user.api.controller.UserFeignApi;
import wcg.yhm.user.api.domain.dto.GetUserListByIdsRequest;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户服务远程调用
 */
@Component
@Slf4j
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    public List<FindUserResponse> batchQueryUser(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }

        Set<Long> distinctUserIds = new LinkedHashSet<>(userIds);
        List<Long> ids = new ArrayList<>(distinctUserIds);
        List<FindUserResponse> result = new ArrayList<>();

        for (int i = 0; i < ids.size(); i += 10) {
            int end = Math.min(i + 10, ids.size());
            GetUserListByIdsRequest request = new GetUserListByIdsRequest(ids.subList(i, end));
            Response<List<FindUserResponse>> response = userFeignApi.batchQueryUser(request);
            if (response == null || !response.isSuccess()) {
                log.warn("批量查询用户失败, userIds: {}", request.getIds());
                continue;
            }
            if (CollUtil.isNotEmpty(response.getData())) {
                result.addAll(response.getData());
            }
        }

        return result;
    }
}
