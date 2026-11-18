package wcg.yhm.oss.biz.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.oss.biz.service.FileService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: 文件操作相关接口
 * @Version: 1.0
 */

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> fileUpload(@RequestPart("file") MultipartFile file) {
        log.info("=============== file upload ================");
        String url = fileService.uploadFile(file);
        return Response.success(url);
    }

    @PostMapping(value = "/pptx/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> pptxUpload(@RequestPart("file") MultipartFile file) {
        log.info("=============== pptx upload ================");
        String url = fileService.uploadPptx(file);
        return Response.success(url);
    }
}
    
