package wcg.yhm.ymm.count.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymm.count.api.domain.request.BatchQueryNoteCountRequest;
import wcg.yhm.ymm.count.api.domain.response.NoteCountResponse;
import wcg.yhm.ymm.count.service.INoteCountService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 笔记计数控制器
 * @Version: 1.0
 */

@RestController
@RequestMapping("/count/note")
@Slf4j
public class NoteCountController {
    @Resource
    private INoteCountService noteCountService;

    @ApiOperationLog(description = "批量查询笔记计数")
    @PostMapping("/batch/query")
    public Response<List<NoteCountResponse>> batchQueryNoteCount(@Validated @RequestBody
                                                            BatchQueryNoteCountRequest batchQueryNoteCountRequest) {
        List<NoteCountResponse> noteCountResponses = noteCountService.batchQueryByIds(batchQueryNoteCountRequest.getNoteIds());
        return Response.success(noteCountResponses);

    }
}
    
