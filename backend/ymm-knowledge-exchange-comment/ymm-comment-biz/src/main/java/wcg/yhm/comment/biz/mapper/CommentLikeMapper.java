package wcg.yhm.comment.biz.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.comment.biz.domain.po.CommentLike;

import java.util.List;

/**
 * <p>
 * 评论点赞表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2026-04-03
 */
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    int batchInsertOrUpdate(@Param("commentLikes") List<CommentLike> commentLikes);
}
