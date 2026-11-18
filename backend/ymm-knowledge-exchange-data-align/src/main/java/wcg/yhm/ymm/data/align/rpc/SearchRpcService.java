package wcg.yhm.ymm.data.align.rpc;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.search.api.controller.SearchFeignApi;
import wcg.yhm.ymm.search.api.domain.request.RebuildNoteDocumentRequest;
import wcg.yhm.ymm.search.api.domain.request.RebuildUserDocumentRequest;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description: 搜索服务远程调用
 * @Version: 1.0
 */

@Component
@Slf4j
public class SearchRpcService {
    @Resource
    private SearchFeignApi searchFeignApi;

    /**
     * 调用重建笔记文档接口
     * @param noteId
     */
    public void rebuildNoteDocument(Long noteId) {
        RebuildNoteDocumentRequest request = new RebuildNoteDocumentRequest(noteId);
        searchFeignApi.reBuildNoteDocument(request);
    }

    /**
     * 调用重建用户文档接口
     * @param userId
     */
    public void rebuildUserDocument(Long userId) {
        RebuildUserDocumentRequest request = new RebuildUserDocumentRequest(userId);
        searchFeignApi.rebuildUserDocument(request);
    }
}
    
