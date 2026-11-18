package wcg.yhm.ymm.search.biz.service;

import org.springframework.http.ResponseEntity;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-01
 * @Description: 自定义字典服务
 * @Version: 1.0
 */

public interface ExtDictService {

    /**
     * 从Linux中获取热更新词典
     */
    ResponseEntity<String> getHotUpdateExtDict();
}
    
