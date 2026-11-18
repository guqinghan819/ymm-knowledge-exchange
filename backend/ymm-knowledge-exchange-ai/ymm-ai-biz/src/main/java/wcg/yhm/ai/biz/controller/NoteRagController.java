package wcg.yhm.ai.biz.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import wcg.yhm.ai.biz.domain.model.NoteRagRetrieveResult;
import wcg.yhm.ai.biz.domain.request.NoteRagChatRequest;
import wcg.yhm.ai.biz.domain.request.NoteRagRetrieveRequest;
import wcg.yhm.ai.biz.domain.response.NoteRagRetrieveResponse;
import wcg.yhm.ai.biz.enums.ErrorCodeEnum;
import wcg.yhm.ai.biz.service.INoteRagChatService;
import wcg.yhm.ai.biz.service.INoteRagRetrieveService;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;
import java.util.Objects;

/**
 * 笔记 RAG 问答控制器。
 */
@RestController
@RequestMapping("/agent/note-rag")
public class NoteRagController {

    @Resource
    private INoteRagChatService noteRagChatService;

    @Resource
    private INoteRagRetrieveService noteRagRetrieveService;

    @PostMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chatStream(@RequestBody @Validated NoteRagChatRequest request,
                                   HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");
        return noteRagChatService.chatStream(request);
    }

    @PostMapping("/retrieve")
    public Response<NoteRagRetrieveResponse> retrieve(@RequestBody @Validated NoteRagRetrieveRequest request) {
        if (Objects.isNull(UserContextHolder.getUserId())) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        NoteRagRetrieveResult result = noteRagRetrieveService.retrieve(request.getQuery());
        NoteRagRetrieveResponse response = NoteRagRetrieveResponse.builder()
                .query(result.getQuery())
                .queries(result.getQueries())
                .standaloneQuery(result.getStandaloneQuery())
                .exactTerms(result.getExactTerms())
                .answerable(result.isAnswerable())
                .evidenceReason(result.getEvidenceReason())
                .candidates(result.getCandidates())
                .references(result.getReferences() == null ? List.of() : result.getReferences())
                .build();
        return Response.success(response);
    }
}
