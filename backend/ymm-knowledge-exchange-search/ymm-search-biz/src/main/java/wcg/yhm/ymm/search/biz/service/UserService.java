package wcg.yhm.ymm.search.biz.service;


import wcg.yhm.ymm.search.api.domain.request.RebuildNoteDocumentRequest;
import wcg.yhm.ymm.search.api.domain.request.RebuildUserDocumentRequest;
import wcg.yhm.ymm.search.biz.domain.request.SearchUserRequest;
import wcg.yhm.ymm.search.biz.domain.response.SearchUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description:
 * @Version: 1.0
 */

public interface UserService {

    /**
     * 查询用户列表
     * @param searchUserRequest 请求
     * @return 列表
     */
    PageResponse<SearchUserResponse> searchUser(SearchUserRequest searchUserRequest);

    /**
     * 重建用户文档
     * @param request 用户id
     */
    void rebuildDocument(RebuildUserDocumentRequest request);
}
    
