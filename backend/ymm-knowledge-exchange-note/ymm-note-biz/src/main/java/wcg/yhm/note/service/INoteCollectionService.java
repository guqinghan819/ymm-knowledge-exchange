package wcg.yhm.note.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.note.domain.dataobject.NoteCollection;
import wcg.yhm.note.model.dto.NoteCollectRequest;
import wcg.yhm.note.model.dto.NoteUnCollectRequest;

/**
 * <p>
 * 笔记收藏表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface INoteCollectionService extends IService<NoteCollection> {

    /**
     * 笔记收藏
     * @param noteCollectRequest 笔记id
     */
    void noteCollect(NoteCollectRequest noteCollectRequest);

    /**
     * 笔记取消收藏
     * @param noteUnCollectRequest 笔记id
     */
    void noteUnCollect(NoteUnCollectRequest noteUnCollectRequest);
}
