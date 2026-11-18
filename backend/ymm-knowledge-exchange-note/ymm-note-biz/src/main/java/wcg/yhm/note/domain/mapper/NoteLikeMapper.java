package wcg.yhm.note.domain.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.note.domain.dataobject.NoteLike;

import java.util.List;

/**
 * <p>
 * 笔记点赞表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface NoteLikeMapper extends BaseMapper<NoteLike> {

    /**
     * 插入或者更新
     *
     * @param noteLike
     * @return
     */
    boolean insertOrUpdate(NoteLike noteLike);

    /**
     * 批量插入或更新
     * @param noteLikeList
     * @return
     */
    int batchInsertOrUpdate(@Param("noteLikeList") List<NoteLike> noteLikeList);
}
