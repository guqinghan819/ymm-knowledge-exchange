package wcg.yhm.ai.biz.rpc;

import jakarta.annotation.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import wcg.yhm.oss.api.controller.FileFeignApi;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-28
 * @Description: 对象服务远程调用
 * @Version: 1.0
 */

@Component
public class OssRpcService {

    @Resource
    private FileFeignApi fileFeignApi;

    public String uploadFile(MultipartFile file) {
        Response<?> response = fileFeignApi.fileUpload(file);
        if (!response.isSuccess()) {
            throw new RuntimeException("文件上传失败: " + response.getMessage());
        }
        return (String) response.getData();
    }

    /**
     * 上传字节数组到OSS（RPC方式）
     */
    public String uploadBytes(byte[] bytes, String fileName) {
        MultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "image/png",
                bytes
        );
        return uploadFile(file);
    }

    /**
     * 上传PPTX文件到OSS（RPC方式，走专用接口）
     */
    public String uploadPptx(byte[] bytes, String fileName) {
        MultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                bytes
        );
        Response<?> response = fileFeignApi.pptxUpload(file);
        if (!response.isSuccess()) {
            throw new RuntimeException("PPTX上传失败: " + response.getMessage());
        }
        return (String) response.getData();
    }
}
    
