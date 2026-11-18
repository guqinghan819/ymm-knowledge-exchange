package wcg.yhm.ymm.count.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.ymm.count.domain.dto.AggregationCommentLikeDTO;
import wcg.yhm.ymm.count.domain.po.Comment;


import java.util.List;

/**
 * <p>
 * 评论表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2026-04-03
 */
public interface CommentMapper extends BaseMapper<Comment> {


    /**
     * 更新一级评论的子评论总数
     * @param parentId
     * @param count
     * @return
     */
    int updateChildCommentTotal(@Param("parentId") Long parentId, @Param("count") int count);

    int batchUpdateCommentLikeTotal(@Param("commentLikeCounts") List<AggregationCommentLikeDTO> commentLikeCounts);
}
