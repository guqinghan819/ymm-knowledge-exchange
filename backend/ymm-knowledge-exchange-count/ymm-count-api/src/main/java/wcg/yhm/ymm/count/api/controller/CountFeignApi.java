package wcg.yhm.ymm.count.api.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import wcg.yhm.ymm.count.api.constant.ServiceNameConstant;
import wcg.yhm.ymm.count.api.domain.request.BatchQueryNoteCountRequest;
import wcg.yhm.ymm.count.api.domain.request.QueryUserCountRequest;
import wcg.yhm.ymm.count.api.domain.response.NoteCountResponse;
import wcg.yhm.ymm.count.api.domain.response.UserCountResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-14
 * @Description: 计数远程调用
 * @Version: 1.0
 */

@FeignClient(name = ServiceNameConstant.SERVICE_NAME)
public interface CountFeignApi {

    String PATH_PREFIX_USER = "/count/user";

    String PATH_PREFIX_NOTE = "/count/note";

    @PostMapping(value = PATH_PREFIX_USER + "/get")
    Response<UserCountResponse> getUserCount(@Validated @RequestBody QueryUserCountRequest queryUserCountRequest);

    @PostMapping(value = PATH_PREFIX_NOTE + "/batch/query")
    Response<List<NoteCountResponse>> batchQueryNoteCount(@Validated @RequestBody
                                                                 BatchQueryNoteCountRequest batchQueryNoteCountRequest);
}
    
