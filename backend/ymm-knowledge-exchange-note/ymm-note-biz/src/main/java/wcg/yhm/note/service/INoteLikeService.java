package wcg.yhm.note.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.note.domain.dataobject.NoteLike;
import wcg.yhm.note.model.dto.NoteLikeRequest;
import wcg.yhm.note.model.dto.NoteUnlikeRequest;

/**
 * <p>
 * 笔记点赞表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface INoteLikeService extends IService<NoteLike> {

    /**
     * 笔记点赞
     * @param noteLikeRequest 笔记id
     */
    void noteLike(NoteLikeRequest noteLikeRequest);

    /**
     * 笔记取消点赞
     * @param noteUnlikeRequest 笔记id
     */
    void noteUnlike(NoteUnlikeRequest noteUnlikeRequest);
}
