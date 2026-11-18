package wcg.yhm.comment.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.comment.biz.domain.request.PublishCommentRequest;
import wcg.yhm.comment.biz.domain.request.QueryCommentListRequest;
import wcg.yhm.comment.biz.domain.request.QueryReplyCommentListRequest;
import wcg.yhm.comment.biz.domain.response.CommentItemResponse;
import wcg.yhm.comment.biz.domain.response.PublishCommentResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;

/**
 * <p>
 * 评论表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-04-03
 */
public interface ICommentService extends IService<Comment> {

    /**
     * 评论发布
     * @param publishCommentRequest
     */
    PublishCommentResponse publishComment(PublishCommentRequest publishCommentRequest);

    /**
     * 查询一级评论列表
     * @param request 查询请求
     * @return 评论分页
     */
    CursorPageResponse<CommentItemResponse> listComments(QueryCommentListRequest request);

    /**
     * 查询二级评论列表
     * @param request 查询请求
     * @return 评论分页
     */
    CursorPageResponse<CommentItemResponse> listReplyComments(QueryReplyCommentListRequest request);
}
