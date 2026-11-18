package wcg.yhm.note.controller;

import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.note.model.dto.AdminNoteListRequest;
import wcg.yhm.note.model.dto.AdminNoteOperateRequest;
import wcg.yhm.note.model.dto.AdminDownNoteRequest;
import wcg.yhm.note.model.dto.FindNoteDetailRequest;
import wcg.yhm.note.enums.AdminDownReasonEnum;
import wcg.yhm.note.model.vo.AdminDownReasonResponse;
import wcg.yhm.note.model.vo.AdminNoteItemResponse;
import wcg.yhm.note.model.vo.FindNoteDetailResponse;
import wcg.yhm.note.model.vo.AdminDashboardResponse;
import wcg.yhm.note.service.IAdminDashboardService;
import wcg.yhm.note.service.INoteService;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 平台管理员笔记管理接口
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/note/admin")
public class NoteAdminController {

    @Resource
    private INoteService noteService;

    @Resource
    private IAdminDashboardService adminDashboardService;

    /**
     * 管理端分析面板。
     */
    @GetMapping("/analysis")
    @ApiOperationLog(description = "管理端分析面板")
    public Response<AdminDashboardResponse> analysis() {
        return Response.success(adminDashboardService.analysis());
    }

    /**
     * 管理端笔记列表(可按状态/作者筛选，含待审核、已下架等全部状态)
     */
    @PostMapping("/list")
    @ApiOperationLog(description = "管理端笔记列表")
    public CursorPageResponse<AdminNoteItemResponse> adminNoteList(@RequestBody AdminNoteListRequest request) {
        return noteService.adminNoteList(request);
    }

    @PostMapping("/detail")
    @ApiOperationLog(description = "管理端查看笔记详情")
    public Response<FindNoteDetailResponse> adminNoteDetail(@Validated @RequestBody FindNoteDetailRequest request) {
        return Response.success(noteService.adminNoteDetail(request));
    }

    /**
     * 笔记审核通过
     */
    @PostMapping("/check/pass")
    @ApiOperationLog(description = "笔记审核通过")
    public Response<?> checkPass(@Validated @RequestBody AdminNoteOperateRequest request) {
        noteService.noteCheckPass(request.getNoteId());
        return Response.success();
    }

    /**
     * 笔记审核驳回
     */
    @PostMapping("/check/reject")
    @ApiOperationLog(description = "笔记审核驳回")
    public Response<?> checkReject(@Validated @RequestBody AdminNoteOperateRequest request) {
        noteService.adminCheckReject(request.getNoteId(), request.getReason());
        return Response.success();
    }

    /**
     * 下架笔记
     */
    @GetMapping("/down/reasons")
    @ApiOperationLog(description = "获取笔记下架原因")
    public Response<List<AdminDownReasonResponse>> downReasons() {
        List<AdminDownReasonResponse> reasons = Arrays.stream(AdminDownReasonEnum.values())
                .map(item -> new AdminDownReasonResponse(item.name(), item.getLabel()))
                .toList();
        return Response.success(reasons);
    }

    @PostMapping("/down")
    @ApiOperationLog(description = "下架笔记")
    public Response<?> down(@Validated @RequestBody AdminDownNoteRequest request) {
        noteService.adminDownNote(request);
        return Response.success();
    }

    /**
     * 恢复上架笔记
     */
    @PostMapping("/restore")
    @ApiOperationLog(description = "恢复上架笔记")
    public Response<?> restore(@Validated @RequestBody AdminNoteOperateRequest request) {
        noteService.adminRestoreNote(request.getNoteId());
        return Response.success();
    }

    /**
     * 强制删除任意笔记
     */
    @PostMapping("/delete")
    @ApiOperationLog(description = "强制删除笔记")
    public Response<?> delete(@Validated @RequestBody AdminNoteOperateRequest request) {
        noteService.adminDeleteNote(request.getNoteId());
        return Response.success();
    }
}
