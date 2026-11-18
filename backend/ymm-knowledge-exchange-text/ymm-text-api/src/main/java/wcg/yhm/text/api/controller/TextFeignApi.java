package wcg.yhm.text.api.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import wcg.yhm.text.api.constant.ServiceNameConstant;
import wcg.yhm.text.api.domain.dto.*;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description:
 * @Version: 1.0
 */

@FeignClient(name = ServiceNameConstant.SERVICE_NAME)
public interface TextFeignApi {

    String PATH_PREFIX = "/text";

    @PostMapping(value = PATH_PREFIX + "/add/note")
    Response<?> addContent(@RequestBody AddNoteContentRequest addNoteContentRequest);

    @PostMapping(value = PATH_PREFIX + "/find/content/byId")
    Response<QueryByIdResponse> findContentById(@RequestBody QueryByIdRequest queryByIdRequest);

    @PostMapping(value = PATH_PREFIX + "/delete/content")
    Response<?> delete(@RequestBody @Validated DeleteRequest deleteRequest);

    @PostMapping(value = PATH_PREFIX + "/comment/batch/add")
    Response<?> batchAddComment(@Validated @RequestBody BatchAddCommentRequest batchAddCommentRequest);

    @PostMapping(value = PATH_PREFIX + "/comment/batch/query")
    Response<java.util.List<FindCommentContentResponse>> batchQueryCommentContent(
            @Validated @RequestBody BatchFindCommentContentRequest batchFindCommentContentRequest);
}
    
