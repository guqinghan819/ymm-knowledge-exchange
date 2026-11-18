package wcg.yhm.note.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.model.dto.*;
import wcg.yhm.note.model.vo.AdminNoteItemResponse;
import wcg.yhm.note.model.vo.FindNoteDetailResponse;
import wcg.yhm.note.model.vo.MyNoteItemResponse;
import wcg.yhm.note.model.vo.MyNotesPageResponse;
import wcg.yhm.note.model.vo.NoteAnalysisResponse;
import wcg.yhm.note.model.vo.NoteItemResponse;
import wcg.yhm.note.model.vo.UserInteractionSummaryResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;

import java.util.List;

/**
 * <p>
 * 笔记表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
public interface INoteService extends IService<Note> {

    /**
     * 发布笔记
     * @param publishNoteRequest
     */
    void publishNote(PublishNoteRequest publishNoteRequest);

    /**
     * 获取笔记详情
     * @param findNoteDetailRequest 请求类
     * @return 笔记详情
     */
    FindNoteDetailResponse findNoteDetail(FindNoteDetailRequest findNoteDetailRequest);

    /** 管理员查看任意状态的笔记详情。 */
    FindNoteDetailResponse adminNoteDetail(FindNoteDetailRequest findNoteDetailRequest);

    /**
     * 查询 RAG 索引用笔记详情。
     */
    NoteRagIndexDetailResponse getRagIndexDetail(Long noteId);

    /**
     * 更新笔记
     * @param updateNoteRequest 更新请求
     */
    void updateNote(UpdateNoteRequest updateNoteRequest);

    /**
     * 删除笔记
     * @param deleteNoteRequest 删除请求
     */
    void deleteNote(DeleteNoteRequest deleteNoteRequest);

    /**
     * 设置私密
     * @param setVisibleRequest 设置私密请求
     */
    void setVisibleOnly(SetVisibleRequest setVisibleRequest);

    /**
     * 设置笔记置顶
     * @param setTopRequest 置顶请求
     */
    void setTop(SetTopRequest setTopRequest);

    /**
     * 游标分页查询下一页
     * @param cursor 上一页最后一条记录id
     * @param limit 每页记录条数
     * @param channelId 频道id
     * @param userId 用户id
     * @return
     */
    CursorPageResponse<NoteItemResponse> getNextPage(Long cursor, int limit, Long channelId, Long userId);

    List<NoteItemResponse> batchQueryNoteItems(List<Long> noteIds);


    /**
     * 查询已经收藏了的笔记
     * @param cursor
     * @param limit
     * @param userId
     * @return
     */
    CursorPageResponse<NoteItemResponse> getCollectNote(Long cursor, int limit);

    /**
     * 查询当前登录用户点赞过的笔记
     */
    CursorPageResponse<NoteItemResponse> getLikeNote(Long cursor, int limit);

    /**
     * 查询当前登录用户主动点赞、收藏的数量
     */
    UserInteractionSummaryResponse getInteractionSummary();


    /**
     * 笔记审核通过
     * @param noteId 笔记id
     */
    void noteCheckPass(Long noteId);

    /**
     * 查询当前用户的笔记（按审核状态筛选）
     */
    MyNotesPageResponse queryMyNotes(QueryMyNotesRequest request);

    /**
     * 查询当前登录用户的笔记分析。
     */
    NoteAnalysisResponse analysis();

    /**
     * 管理端笔记列表（可按状态/作者筛选，含全部状态）
     * @param request 列表请求
     * @return 笔记列表
     */
    CursorPageResponse<AdminNoteItemResponse> adminNoteList(AdminNoteListRequest request);

    /**
     * 笔记审核驳回
     * @param noteId 笔记id
     */
    void adminCheckReject(Long noteId, String reason);

    /**
     * 下架笔记
     * @param noteId 笔记id
     */
    void adminDownNote(AdminDownNoteRequest request);

    /**
     * 恢复上架笔记
     * @param noteId 笔记id
     */
    void adminRestoreNote(Long noteId);

    /**
     * 强制删除任意笔记
     * @param noteId 笔记id
     */
    void adminDeleteNote(Long noteId);
}
