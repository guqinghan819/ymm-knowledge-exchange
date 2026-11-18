package wcg.yhm.ymm.search.api.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import wcg.yhm.ymm.search.api.constants.ServiceNameConstant;
import wcg.yhm.ymm.search.api.domain.request.RebuildNoteDocumentRequest;
import wcg.yhm.ymm.search.api.domain.request.RebuildUserDocumentRequest;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description:
 * @Version: 1.0
 */

@FeignClient(name = ServiceNameConstant.SERVICE_NAME)
public interface SearchFeignApi {


    String PATH_PREFIX = "/search";

    @PostMapping(PATH_PREFIX + "/user/rebuild")
    Response<?> rebuildUserDocument(@RequestBody @Validated RebuildUserDocumentRequest request);

    @PostMapping(PATH_PREFIX + "/note/rebuild")
    Response<?> reBuildNoteDocument(@RequestBody @Validated RebuildNoteDocumentRequest request);
}
    
