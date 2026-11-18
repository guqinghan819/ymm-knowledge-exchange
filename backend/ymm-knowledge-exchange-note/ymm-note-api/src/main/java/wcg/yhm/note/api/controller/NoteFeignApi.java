package wcg.yhm.note.api.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import wcg.yhm.note.api.constatnts.ServiceNameConstant;
import wcg.yhm.note.api.domain.request.PublishNoteRequest;
import wcg.yhm.note.api.domain.request.QueryNoteRagIndexDetailRequest;
import wcg.yhm.note.api.domain.request.QueryTopicRequest;
import wcg.yhm.note.api.domain.response.CatalogItemResponse;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-28
 * @Description:
 * @Version: 1.0
 */

@FeignClient(name = ServiceNameConstant.SERVICE_NAME)
public interface NoteFeignApi {

    String PATH_PREFIX = "/note";

    @PostMapping(PATH_PREFIX + "/publish")
    Response<?> publishNote(@RequestBody PublishNoteRequest publishNoteRequest);

    @GetMapping("/channel/get")
    Response<List<CatalogItemResponse>> getChannels();

    @PostMapping("/topic/get")
    Response<List<CatalogItemResponse>> getTopics(@Validated @RequestBody QueryTopicRequest request);

    @PostMapping(PATH_PREFIX + "/rag/index/detail")
    Response<NoteRagIndexDetailResponse> getRagIndexDetail(
            @Validated @RequestBody QueryNoteRagIndexDetailRequest request);
}
    
