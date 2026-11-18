package wcg.yhm.ai.biz.agent.service;

import wcg.yhm.ai.biz.domain.pojo.AiPptInst;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-15
 * @Description:
 * @Version: 1.0
 */

public interface PptPythonRenderService {

    /**
     * 调用Python脚本渲染PPT
     *
     * @param inst       PPT实例
     * @param pptSchema PPT Schema JSON
     * @return 生成的PPT文件URL
     * @throws Exception 渲染失败时抛出异常
     */
    String renderPpt(AiPptInst inst, String pptSchema) throws Exception;
}
    
