package wcg.yhm.user.biz.rpc;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.api.controller.CountFeignApi;
import wcg.yhm.ymm.count.api.domain.request.QueryUserCountRequest;
import wcg.yhm.ymm.count.api.domain.response.UserCountResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-14
 * @Description: 计数远程调用服务
 * @Version: 1.0
 */

@Component
@Slf4j
public class CountRpcService {
    @Resource
    private CountFeignApi countFeignApi;


    public UserCountResponse getUserCount(Long userId) {
        QueryUserCountRequest queryUserCountRequest = new QueryUserCountRequest();
        queryUserCountRequest.setUserId(userId);
        Response<UserCountResponse> response = countFeignApi.getUserCount(queryUserCountRequest);
        if (!response.isSuccess()) {
            return null;
        }
        return response.getData();
    }
}
    
