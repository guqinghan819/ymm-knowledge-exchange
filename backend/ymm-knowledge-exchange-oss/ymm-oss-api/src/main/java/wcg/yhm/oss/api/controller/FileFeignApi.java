package wcg.yhm.oss.api.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import wcg.yhm.oss.api.config.FeignFormConfig;
import wcg.yhm.oss.api.constant.ServiceNameConstant;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-28
 * @Description: 文件调用api
 * @Version: 1.0
 */

@FeignClient(name = ServiceNameConstant.SERVICE_NAME, configuration = FeignFormConfig.class)
public interface FileFeignApi {
    String PATH_PREFIX = "/file";

    @PostMapping(value = PATH_PREFIX + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<?> fileUpload(@RequestPart("file") MultipartFile file);

    @PostMapping(value = PATH_PREFIX + "/pptx/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<?> pptxUpload(@RequestPart("file") MultipartFile file);
}
    
