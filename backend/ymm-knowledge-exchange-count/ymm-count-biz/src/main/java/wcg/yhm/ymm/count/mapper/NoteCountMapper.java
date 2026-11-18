package wcg.yhm.ymm.count.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.ymm.count.domain.po.NoteCount;

import java.util.List;

/**
 * <p>
 * 笔记计数表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface NoteCountMapper extends BaseMapper<NoteCount> {

    /**
     * 批量插入或更新笔记获赞
     * @param noteCounts 记录
     */
    void saveOrUpdateNoteLikeBatch(@Param("noteCounts") List<NoteCount> noteCounts);

    void saveOrUpdateNoteCollectBatch(@Param("noteCounts") List<NoteCount> noteCounts);

    /**
     * 添加记录或更新笔记评论数
     * @param count
     * @param noteId
     * @return
     */
    int insertOrUpdateCommentTotalByNoteId(@Param("count") int count, @Param("noteId") Long noteId);
}
