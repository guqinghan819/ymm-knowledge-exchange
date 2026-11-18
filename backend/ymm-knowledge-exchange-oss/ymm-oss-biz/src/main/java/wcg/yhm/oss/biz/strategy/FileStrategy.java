package wcg.yhm.oss.biz.strategy;

import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: 文件存储策略接口 提供文件操作接口
 * @Version: 1.0
 */

public interface FileStrategy {
    /**
     * 上传文件
     * @param file 文件
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file);

    /**
     * 上传PPTX文件（存放于pptx/专用目录）
     * @param file PPTX文件
     * @return 文件访问URL
     */
    String uploadPptx(MultipartFile file);
}
    
