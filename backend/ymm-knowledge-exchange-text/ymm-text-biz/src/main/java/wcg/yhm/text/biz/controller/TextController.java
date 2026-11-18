package wcg.yhm.text.biz.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.text.api.domain.dto.*;
import wcg.yhm.text.biz.service.TextService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description:
 * @Version: 1.0
 */

@RestController
@RequestMapping("/text")
@Slf4j
public class TextController {
    @Resource
    private TextService textService;

    @PostMapping("/add/note")
    public Response<?> addContent(@RequestBody @Validated AddNoteContentRequest addNoteContentRequest) {
        log.info("================ addContent ===============");
        textService.addText(addNoteContentRequest);
        return Response.success();
    }

    @PostMapping("/find/content/byId")
    public Response<QueryByIdResponse> findContentById(@RequestBody @Validated QueryByIdRequest queryByIdRequest) {
        log.info("================ findContentById ===============");
        QueryByIdResponse queryByIdResponse = textService.queryById(queryByIdRequest);
        return Response.success(queryByIdResponse);
    }

    @PostMapping("/delete/content")
    public Response<?> delete(@RequestBody @Validated DeleteRequest deleteRequest) {
        log.info("================ deleteContent ===============");
        textService.delete(deleteRequest);
        return Response.success();
    }

    @PostMapping("/comment/batch/add")
    @ApiOperationLog(description = "批量添加评论正文")
    public Response<?> batchAddComment(@Validated @RequestBody BatchAddCommentRequest batchAddCommentRequest) {
        textService.batchAddComment(batchAddCommentRequest);
        return Response.success();
    }


    @PostMapping("/comment/batch/query")
    @ApiOperationLog(description = "批量查询评论内容")
    public Response<List<FindCommentContentResponse>> batchQueryCommentContent(@Validated @RequestBody BatchFindCommentContentRequest batchFindCommentContentRequest) {
        List<FindCommentContentResponse> findCommentContentResponseList = textService.batchFindCommentContent(batchFindCommentContentRequest);
        return Response.success(findCommentContentResponseList);
    }
}
    
