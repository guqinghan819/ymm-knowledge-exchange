package wcg.yhm.text.biz.service;

import org.w3c.dom.stylesheets.LinkStyle;
import wcg.yhm.text.api.domain.dto.*;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: 文本存储接口
 * @Version: 1.0
 */

public interface TextService {

    /**
     * 新增内容
     * @param addNoteContentRequest
     */
    void addText(AddNoteContentRequest addNoteContentRequest);

    /**
     * 根据id查询内容
     * @param queryByIdRequest
     * @return
     */
    QueryByIdResponse queryById(QueryByIdRequest queryByIdRequest);

    /**
     * 删除内容
     * @param deleteRequest
     */
    void delete(DeleteRequest deleteRequest);

    /**
     * 批量新增评论内容
     */
    void batchAddComment(BatchAddCommentRequest batchAddCommentRequest);

    /**
     * 批量查询评论内容
     * @param batchFindCommentContentRequest
     * @return
     */
    List<FindCommentContentResponse> batchFindCommentContent(BatchFindCommentContentRequest batchFindCommentContentRequest);


}
    
