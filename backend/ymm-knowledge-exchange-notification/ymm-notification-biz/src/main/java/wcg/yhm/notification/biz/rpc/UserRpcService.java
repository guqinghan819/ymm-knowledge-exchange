package wcg.yhm.notification.biz.rpc;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.user.api.controller.UserFeignApi;
import wcg.yhm.user.api.domain.dto.GetUserListByIdsRequest;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    public List<FindUserResponse> findByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Response<List<FindUserResponse>> response = userFeignApi.batchQueryUser(new GetUserListByIdsRequest(userIds));
            if (response == null || !response.isSuccess() || response.getData() == null) {
                return Collections.emptyList();
            }
            return response.getData();
        } catch (Exception e) {
            log.warn("批量查询通知行为人失败, userIds={}", userIds, e);
            return Collections.emptyList();
        }
    }
}
