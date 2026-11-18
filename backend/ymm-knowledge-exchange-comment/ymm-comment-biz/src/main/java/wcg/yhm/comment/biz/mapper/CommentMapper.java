package wcg.yhm.comment.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.comment.biz.domain.bo.CommentBO;
import wcg.yhm.comment.biz.domain.bo.CommentHeatBO;
import wcg.yhm.comment.biz.domain.po.Comment;

import java.math.BigDecimal;
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
     * 批量插入评论
     * @param comments
     * @return
     */
    int batchInsert(@Param("comments") List<CommentBO> comments);

    /**
     * 批量更新热度值
     * @param commentIds
     * @param commentHeatBOS
     * @return
     */
    int batchUpdateHeatByCommentIds(@Param("commentIds") List<Long> commentIds,
                                    @Param("commentHeatBOS") List<CommentHeatBO> commentHeatBOS);

    /**
     * 游标查询一级评论
     * @param noteId 笔记 ID
     * @param cursor 游标
     * @param limit 每页数量
     * @return 评论列表
     */
    List<Comment> selectOneLevelByCursor(@Param("noteId") Long noteId,
                                         @Param("cursor") Long cursor,
                                         @Param("cursorHeat") BigDecimal cursorHeat,
                                         @Param("limit") Integer limit);

    /**
     * 游标查询二级评论
     * @param noteId 笔记 ID
     * @param parentId 一级评论 ID
     * @param cursor 游标
     * @param limit 每页数量
     * @return 评论列表
     */
    List<Comment> selectReplyByCursor(@Param("noteId") Long noteId,
                                      @Param("parentId") Long parentId,
                                      @Param("cursor") Long cursor,
                                      @Param("limit") Integer limit);
}
