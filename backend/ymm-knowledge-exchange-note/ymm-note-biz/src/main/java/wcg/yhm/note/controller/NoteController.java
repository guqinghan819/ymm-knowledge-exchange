package wcg.yhm.note.controller;


import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.note.api.domain.request.QueryNoteRagIndexDetailRequest;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;
import wcg.yhm.note.model.dto.*;
import wcg.yhm.note.model.vo.FindNoteDetailResponse;
import wcg.yhm.note.model.vo.MyNoteItemResponse;
import wcg.yhm.note.model.vo.MyNotesPageResponse;
import wcg.yhm.note.model.vo.NoteAnalysisResponse;
import wcg.yhm.note.model.vo.NoteItemResponse;
import wcg.yhm.note.model.vo.UserInteractionSummaryResponse;
import wcg.yhm.note.service.INoteCollectionService;
import wcg.yhm.note.service.INoteLikeService;
import wcg.yhm.note.service.INoteService;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * <p>
 * 笔记表 前端控制器
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/note")
public class NoteController {

    @Resource
    private INoteService noteService;
    @Resource
    private INoteLikeService noteLikeService;
    @Resource
    private INoteCollectionService noteCollectionService;

    @PostMapping("/publish")
    @ApiOperationLog(description = "发布笔记接口")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteRequest publishNoteRequest) {
        noteService.publishNote(publishNoteRequest);
        return Response.success();
    }

    @PostMapping("/detail")
    @ApiOperationLog(description = "获取笔记详情接口")
    public Response<FindNoteDetailResponse> findNoteDetail(@Validated @RequestBody FindNoteDetailRequest findNoteDetailRequest) {
        FindNoteDetailResponse noteDetail = noteService.findNoteDetail(findNoteDetailRequest);
        return Response.success(noteDetail);
    }

    @PostMapping("/item/batch")
    @ApiOperationLog(description = "批量获取笔记卡片详情")
    public Response<List<NoteItemResponse>> batchQueryNoteItems(@RequestBody BatchQueryNoteItemRequest request) {
        List<Long> noteIds = request == null ? List.of() : request.getNoteIds();
        return Response.success(noteService.batchQueryNoteItems(noteIds));
    }

    @PostMapping("/rag/index/detail")
    @ApiOperationLog(description = "获取RAG索引用笔记详情")
    public Response<NoteRagIndexDetailResponse> getRagIndexDetail(
            @Validated @RequestBody QueryNoteRagIndexDetailRequest request) {
        return Response.success(noteService.getRagIndexDetail(request.getNoteId()));
    }

    @PostMapping("/update")
    @ApiOperationLog(description = "更新笔记")
    public Response<?> updateNote(@Validated @RequestBody UpdateNoteRequest updateNoteRequest) {
        noteService.updateNote(updateNoteRequest);
        return Response.success();
    }


    @PostMapping("/delete")
    @ApiOperationLog(description = "删除笔记")
    public Response<?> deleteNote(@Validated @RequestBody DeleteNoteRequest deleteNoteRequest) {
        noteService.deleteNote(deleteNoteRequest);
        return Response.success();
    }

    @PostMapping("/setVisible")
    @ApiOperationLog(description = "设置笔记私密")
    public Response<?> setVisible(@Validated @RequestBody SetVisibleRequest setVisibleRequest) {
        noteService.setVisibleOnly(setVisibleRequest);
        return Response.success();
    }

    @PostMapping("/setTop")
    @ApiOperationLog(description = "设置笔记置顶")
    public Response<?> setTop(@Validated @RequestBody SetTopRequest setTopRequest) {
        noteService.setTop(setTopRequest);
        return Response.success();
    }

    @PostMapping("/like")
    @ApiOperationLog(description = "笔记点赞")
    public Response<?> noteLike(@Validated @RequestBody NoteLikeRequest noteLikeRequest) {
        noteLikeService.noteLike(noteLikeRequest);
        return Response.success();
    }

    @PostMapping("/unlike")
    @ApiOperationLog(description = "笔记取消点赞")
    public Response<?> noteUnLike(@Validated @RequestBody NoteUnlikeRequest noteUnlikeRequest) {
        noteLikeService.noteUnlike(noteUnlikeRequest);
        return Response.success();
    }

    @PostMapping("/collect")
    @ApiOperationLog(description = "笔记收藏")
    public Response<?> noteCollect(@Validated @RequestBody NoteCollectRequest noteCollectRequest) {
        noteCollectionService.noteCollect(noteCollectRequest);
        return Response.success();
    }

    @PostMapping("/UnCollect")
    @ApiOperationLog(description = "笔记收藏")
    public Response<?> noteUnCollect(@Validated @RequestBody NoteUnCollectRequest noteUnCollectRequest) {
        noteCollectionService.noteUnCollect(noteUnCollectRequest);
        return Response.success();
    }


    @PostMapping("/cursor/get")
    @ApiOperationLog(description = "游标获取笔记")
    public CursorPageResponse<NoteItemResponse> selectNoteByCursor(@RequestBody SelectNoteRequest selectNoteRequest) {
        return noteService.getNextPage(selectNoteRequest.getCursor(), selectNoteRequest.getLimit(),
                selectNoteRequest.getChannelId(), selectNoteRequest.getUserId());
    }

    @PostMapping("/collect/cursor/get")
    @ApiOperationLog(description = "游标获取当前用户收藏的笔记")
    public CursorPageResponse<NoteItemResponse> selectCollectedNoteByCursor(@RequestBody SelectNoteRequest request) {
        return noteService.getCollectNote(request.getCursor(), request.getLimit());
    }

    @PostMapping("/like/cursor/get")
    @ApiOperationLog(description = "游标获取当前用户点赞的笔记")
    public CursorPageResponse<NoteItemResponse> selectLikedNoteByCursor(@RequestBody SelectNoteRequest request) {
        return noteService.getLikeNote(request.getCursor(), request.getLimit());
    }

    @GetMapping("/interaction/summary")
    @ApiOperationLog(description = "获取当前用户点赞收藏数量")
    public Response<UserInteractionSummaryResponse> getInteractionSummary() {
        return Response.success(noteService.getInteractionSummary());
    }

    @PostMapping("/my/list")
    @ApiOperationLog(description = "管理端获取当前用户笔记列表")
    public Response<MyNotesPageResponse> queryMyNotes(
            @Validated @RequestBody QueryMyNotesRequest request) {
        return Response.success(noteService.queryMyNotes(request));
    }

    @PostMapping("/analysis")
    @ApiOperationLog(description = "当前用户笔记分析")
    public Response<NoteAnalysisResponse> analysis() {
        return Response.success(noteService.analysis());
    }
}
