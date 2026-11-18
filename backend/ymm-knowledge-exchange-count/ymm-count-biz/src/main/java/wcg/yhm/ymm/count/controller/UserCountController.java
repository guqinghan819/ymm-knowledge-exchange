package wcg.yhm.ymm.count.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymm.count.api.domain.request.QueryUserCountRequest;
import wcg.yhm.ymm.count.api.domain.response.UserCountResponse;
import wcg.yhm.ymm.count.service.IUserCountService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-14
 * @Description:
 * @Version: 1.0
 */

@RestController
@Slf4j
@RequestMapping("/count/user")
public class UserCountController {
    @Resource
    private IUserCountService userCountService;

    @ApiOperationLog(description = "根据用户id查询计数数据")
    @PostMapping("/get")
    public Response<UserCountResponse> getUserCount(@Validated @RequestBody QueryUserCountRequest queryUserCountRequest) {
        UserCountResponse userCount = userCountService.getUserCount(queryUserCountRequest);
        return Response.success(userCount);
    }
}
    
