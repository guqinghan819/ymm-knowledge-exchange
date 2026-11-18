package wcg.yhm.note.domain.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import wcg.yhm.note.domain.dataobject.NoteCollection;

/**
 * <p>
 * 笔记收藏表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface NoteCollectionMapper extends BaseMapper<NoteCollection> {

    /**
     * 插入或更新收藏
     * @param noteCollection
     */
    boolean saveOrUpdateCollect(NoteCollection noteCollection);
}
