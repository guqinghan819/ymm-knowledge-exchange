package wcg.yhm.oss.biz.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: 文件服务
 * @Version: 1.0
 */

public interface FileService {

    /**
     * 文件上传
     * @param file 文件
     */
    String uploadFile(MultipartFile file);

    /**
     * PPTX文件上传
     * @param file PPTX文件
     */
    String uploadPptx(MultipartFile file);
}
    
