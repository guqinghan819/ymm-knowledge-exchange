package wcg.yhm.oss.biz.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import wcg.yhm.oss.biz.factory.FileStrategyFactory;
import wcg.yhm.oss.biz.service.FileService;
import wcg.yhm.oss.biz.strategy.FileStrategy;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description:
 * @Version: 1.0
 */

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Resource
   private FileStrategy fileStrategy;
    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null) {
            throw new RuntimeException("文件不能为空");
        }
        return fileStrategy.uploadFile(file);
    }

    @Override
    public String uploadPptx(MultipartFile file) {
        if (file == null) {
            throw new RuntimeException("PPTX文件不能为空");
        }
        return fileStrategy.uploadPptx(file);
    }
}
    
