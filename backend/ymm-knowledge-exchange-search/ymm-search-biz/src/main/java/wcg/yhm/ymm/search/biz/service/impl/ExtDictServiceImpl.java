package wcg.yhm.ymm.search.biz.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import wcg.yhm.ymm.search.biz.service.ExtDictService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-01
 * @Description:
 * @Version: 1.0
 */

@Slf4j
@Service
public class ExtDictServiceImpl implements ExtDictService {

    @Value("${elasticsearch.hotUpdateExtDict}")
    private String hotUpdateExtDict; //文件地址


    @Override
    public ResponseEntity<String> getHotUpdateExtDict() {
        try {
            Path path = Paths.get(hotUpdateExtDict);
            //获取文件最后的更新时间
            long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();
            //生成 ETag(使用文件内容的哈希值)
            String fileContent = Files.lines(path).collect(Collectors.joining("\n"));
            String eTag = String.valueOf(fileContent.hashCode());
            //设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.set("ETag", eTag);
            //设置内容类型为UTF-8
            headers.setContentType(MediaType.valueOf("text/plain;charset=UTF-8"));
            return ResponseEntity.ok()
                    .headers(headers)
                    .lastModified(lastModifiedTime)
                    .body(fileContent);
        } catch (IOException e) {
            log.error("## 获取热更新词典异常: ", e);
        }
        return null;
    }
}
    
