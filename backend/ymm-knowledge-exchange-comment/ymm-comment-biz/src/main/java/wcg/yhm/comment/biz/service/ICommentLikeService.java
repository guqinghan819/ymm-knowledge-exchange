package wcg.yhm.comment.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.comment.biz.domain.po.CommentLike;

/**
 * <p>
 * 评论点赞表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-04-03
 */
public interface ICommentLikeService extends IService<CommentLike> {

    void likeComment(Long commentId);

    void unlikeComment(Long commentId);
}
