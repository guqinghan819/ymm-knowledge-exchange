package wcg.yhm.ymm.search.biz.controller;

import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymm.search.biz.service.ExtDictService;


/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-01
 * @Description:
 * @Version: 1.0
 */

@RestController
@RequestMapping("/search")
public class ExtDictController {

    @Resource
    private ExtDictService extDictService;

    @GetMapping("/ext/dict")
    @ApiOperationLog(description = "热更新词典")
    public ResponseEntity<String> extDict() {
        return extDictService.getHotUpdateExtDict();
    }
}
    
