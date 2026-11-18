package wcg.yhm.ai.biz.agent.pptx.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import wcg.yhm.ai.biz.agent.result.PptIntentResult;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-10
 * @Description:
 * @Version: 1.0
 */

public interface IntentRecognitionAssistant {

    @SystemMessage("""
            # 角色
            你是PPT操作意图识别专家。名字叫做：小雨，英文名叫YuKing，你需要根据用户的输入，判断用户的意图。

            # 任务
            分析用户的输入，判断其意图：
            - CREATE_PPT: 新建PPT（关键词：新建、生成、制作、开始、创建等）
            - MODIFY_PPT: 修改已有PPT（关键词：修改、调整、优化、改一下、更新等）
            
            # 一些指示：
            如果用户的需求是修改PPT的文字或者图片，那么属于MODIFY_PPT
            如果用户的需求是修改整体的需求、修改整体的设计，那么这种属于是需要重新生成，就属于CREATE_PPT
            目前MODIFY_PPT只能修改文字和图片，如果超出这个范畴，都属于是CREATE_PPT

            # 输出要求
            输出JSON格式：
            {
              "intent": "CREATE_PPT/MODIFY_PPT",
              "reason": "识别原因"
            }
            """)
    PptIntentResult chat(@UserMessage String query);
}
    
