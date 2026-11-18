package wcg.yhm.comment.biz.controller;

import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.comment.biz.domain.request.CommentLikeRequest;
import wcg.yhm.comment.biz.domain.request.CommentUnlikeRequest;
import wcg.yhm.comment.biz.domain.request.PublishCommentRequest;
import wcg.yhm.comment.biz.domain.request.QueryCommentListRequest;
import wcg.yhm.comment.biz.domain.request.QueryReplyCommentListRequest;
import wcg.yhm.comment.biz.domain.response.CommentItemResponse;
import wcg.yhm.comment.biz.domain.response.PublishCommentResponse;
import wcg.yhm.comment.biz.service.ICommentLikeService;
import wcg.yhm.comment.biz.service.ICommentService;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-03
 * @Description:
 * @Version: 1.0
 */

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Resource
    private ICommentService commentService;
    @Resource
    private ICommentLikeService commentLikeService;

    @ApiOperationLog(description = "评论发布")
    @PostMapping("/publish")
    public Response<PublishCommentResponse> publishComment(@Validated @RequestBody PublishCommentRequest publishCommentRequest) {
        PublishCommentResponse response = commentService.publishComment(publishCommentRequest);
        return Response.success(response);
    }

    @ApiOperationLog(description = "一级评论列表")
    @PostMapping("/list")
    public CursorPageResponse<CommentItemResponse> listComments(@Validated @RequestBody QueryCommentListRequest request) {
        return commentService.listComments(request);
    }

    @ApiOperationLog(description = "二级评论列表")
    @PostMapping("/reply/list")
    public CursorPageResponse<CommentItemResponse> listReplyComments(@Validated @RequestBody QueryReplyCommentListRequest request) {
        return commentService.listReplyComments(request);
    }

    @ApiOperationLog(description = "评论点赞")
    @PostMapping("/like")
    public Response<?> likeComment(@Validated @RequestBody CommentLikeRequest request) {
        commentLikeService.likeComment(request.getCommentId());
        return Response.success();
    }

    @ApiOperationLog(description = "评论取消点赞")
    @PostMapping("/unlike")
    public Response<?> unlikeComment(@Validated @RequestBody CommentUnlikeRequest request) {
        commentLikeService.unlikeComment(request.getCommentId());
        return Response.success();
    }
}
    
