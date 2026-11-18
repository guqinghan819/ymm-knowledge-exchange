package wcg.yhm.ymm.search.biz.service;


import wcg.yhm.ymm.search.api.domain.request.RebuildNoteDocumentRequest;
import wcg.yhm.ymm.search.biz.domain.request.SearchNoteRequest;
import wcg.yhm.ymm.search.biz.domain.response.SearchNoteResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description:
 * @Version: 1.0
 */

public interface NoteService {

    /**
     * 查询笔记
     * @param searchNoteRequest 请求
     * @return 列表
     */
    PageResponse<SearchNoteResponse> searchNote(SearchNoteRequest searchNoteRequest);

    /**
     * 重建笔记文档
     * @param request 笔记id
     * @return
     */
    void rebuildDocument(RebuildNoteDocumentRequest request);
}
    
