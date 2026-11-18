package wcg.yhm.oss.biz.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: 阿里云对象存储策略
 * @Version: 1.0
 */

@Slf4j
@Component("ALIYUN")
public class AliYunOssStrategy implements FileStrategy{

    @Override
    public String uploadFile(MultipartFile file) {
        log.info("==========> AliYunOssStrategy ============");
        return null;
    }

    @Override
    public String uploadPptx(MultipartFile file) {
        return "";
    }
}
    
