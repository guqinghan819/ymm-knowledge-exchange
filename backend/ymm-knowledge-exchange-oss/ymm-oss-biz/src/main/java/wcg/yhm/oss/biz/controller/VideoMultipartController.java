package wcg.yhm.oss.biz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.oss.biz.domain.dto.VideoMultipartInitRequest;
import wcg.yhm.oss.biz.domain.vo.VideoMultipartInitResponse;
import wcg.yhm.oss.biz.domain.vo.VideoMultipartSignResponse;
import wcg.yhm.oss.biz.domain.vo.VideoMultipartStatusResponse;
import wcg.yhm.oss.biz.service.CosVideoMultipartService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

@RestController
@RequestMapping("/file/video/multipart")
@RequiredArgsConstructor
@ConditionalOnExpression("'${strategy.type:}' == 'cos' and ${cos.video.multipart.enabled:false}")
public class VideoMultipartController {

    private final CosVideoMultipartService multipartService;

    @PostMapping("/init")
    public Response<VideoMultipartInitResponse> initialize(@RequestBody VideoMultipartInitRequest request) {
        return Response.success(multipartService.initialize(request));
    }

    @PostMapping("/{taskId}/parts/{partNumber}/sign")
    public Response<VideoMultipartSignResponse> signPart(
            @PathVariable String taskId, @PathVariable int partNumber) {
        return Response.success(multipartService.signPart(taskId, partNumber));
    }

    @GetMapping("/{taskId}/status")
    public Response<VideoMultipartStatusResponse> status(@PathVariable String taskId) {
        return Response.success(multipartService.getStatus(taskId));
    }

    @PostMapping("/{taskId}/complete")
    public Response<String> complete(@PathVariable String taskId) {
        return Response.success(multipartService.complete(taskId));
    }

    @DeleteMapping("/{taskId}")
    public Response<Void> abort(@PathVariable String taskId) {
        multipartService.abort(taskId);
        return Response.success();
    }
}
