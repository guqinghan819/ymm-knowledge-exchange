package wcg.yhm.ymm.search.biz.controller;

import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymm.search.api.domain.request.RebuildNoteDocumentRequest;
import wcg.yhm.ymm.search.biz.domain.request.SearchNoteRequest;
import wcg.yhm.ymm.search.biz.domain.response.SearchNoteResponse;
import wcg.yhm.ymm.search.biz.service.NoteService;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description:
 * @Version: 1.0
 */

@RestController
@RequestMapping("/search")
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping("/note/list")
    @ApiOperationLog(description = "搜索笔记")
    public PageResponse<SearchNoteResponse> searchNote(@RequestBody @Validated SearchNoteRequest searchNoteRequest) {
        return noteService.searchNote(searchNoteRequest);
    }

    @PostMapping("/note/rebuild")
    @ApiOperationLog(description = "重建笔记文档")
    public Response<?> reBuildNoteDocument(@RequestBody @Validated RebuildNoteDocumentRequest request) {
        noteService.rebuildDocument(request);
        return Response.success();
    }
}
    
