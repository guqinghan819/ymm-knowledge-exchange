package wcg.yhm.ymm.search.biz.controller;

import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymm.search.api.domain.request.RebuildUserDocumentRequest;
import wcg.yhm.ymm.search.biz.domain.request.SearchUserRequest;
import wcg.yhm.ymm.search.biz.domain.response.SearchUserResponse;
import wcg.yhm.ymm.search.biz.service.UserService;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description:
 * @Version: 1.0
 */

@RestController
@RequestMapping("/search")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/user/list")
    @ApiOperationLog(description = "es查询用户列表")
    public PageResponse<SearchUserResponse> searchUserList(@Validated @RequestBody SearchUserRequest searchUserRequest) {
        return userService.searchUser(searchUserRequest);
    }

    @PostMapping("/user/rebuild")
    @ApiOperationLog(description = "重构用户文档")
    public Response<?> rebuildUserDocument(@RequestBody @Validated RebuildUserDocumentRequest request) {
        userService.rebuildDocument(request);
        return Response.success();
    }
}
    
