package wcg.yhm.comment.biz.rpc;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.comment.biz.domain.bo.CommentBO;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.text.api.controller.TextFeignApi;
import wcg.yhm.text.api.domain.dto.BatchAddCommentRequest;
import wcg.yhm.text.api.domain.dto.BatchFindCommentContentRequest;
import wcg.yhm.text.api.domain.dto.CommentContentDTO;
import wcg.yhm.text.api.domain.dto.FindCommentContentDTO;
import wcg.yhm.text.api.domain.dto.FindCommentContentResponse;
import wcg.yhm.ymmknowledgeexchange.common.constant.DateConstant;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description:
 * @Version: 1.0
 */

@Component
@Slf4j
public class TextRpcService {
    @Resource
    private TextFeignApi textFeignApi;

    /**
     * 批量添加评论内容
     */
    public Boolean batchAddComment(List<CommentBO> commentBOList) {
        BatchAddCommentRequest batchAddCommentRequest = new BatchAddCommentRequest();
        List<CommentContentDTO> contentDTOList = new ArrayList<>();
        //commentBO -> dto
        commentBOList.forEach(bo -> {
            CommentContentDTO contentDTO = CommentContentDTO.builder()
                    .noteId(bo.getNoteId())
                    .yearMonth(bo.getCreateTime().format(DateConstant.DATE_FORMAT_Y_M))
                    .contentId(bo.getContentUuid())
                    .content(bo.getContent())
                    .build();
            contentDTOList.add(contentDTO);
        });
        batchAddCommentRequest.setContentDTOList(contentDTOList);
        Response<?> response = textFeignApi.batchAddComment(batchAddCommentRequest);
        if (!response.isSuccess()) {
            throw new RuntimeException("批量保存评论内容失败");
        }
        return true;
    }

    /**
     * 批量查询评论内容
     */
    public List<FindCommentContentResponse> batchFindCommentContent(Long noteId, List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        List<FindCommentContentDTO> contentKeys = new ArrayList<>();
        comments.forEach(comment -> {
            FindCommentContentDTO contentDTO = FindCommentContentDTO.builder()
                    .yearMonth(comment.getCreateTime().format(DateConstant.DATE_FORMAT_Y_M))
                    .contentId(comment.getContentUuid())
                    .build();
            contentKeys.add(contentDTO);
        });

        BatchFindCommentContentRequest request = BatchFindCommentContentRequest.builder()
                .noteId(noteId)
                .commentContentKeys(contentKeys)
                .build();
        Response<List<FindCommentContentResponse>> response = textFeignApi.batchQueryCommentContent(request);
        if (response == null || !response.isSuccess()) {
            throw new RuntimeException("批量查询评论内容失败");
        }
        return response.getData();
    }

}
    
