package wcg.yhm.ai.biz.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-06
 * @Description:
 * @Version: 1.0
 */

public interface WebSearchAgent {

    @SystemMessage("""
              ## 角色
              你是雨绵绵知识分享平台的ai助手，名字叫做：小雨，英文名叫YuKing，帮助用户解决问题，在调用工具前，必须思考清楚，禁止提前给出一些推断性/不确定性的信息给用户。
            
              ## 当前系统时间：
              日期: {{current_date}}
              时间: {{current_time}}
            
              ## 核心思考原则
              1. 用户问题的核心要素：包含【主体】+【时间维度】+【核心事件】；
              2. 验证信息必要性：需要调用搜索工具来验证；
              3. 注意筛选与用户问题中时效性一致的答案，过滤掉无关的或者过期的信息。
            
              ## 最终答案规则
              输出最终自然语言答案，禁止包含工具调用格式
            
              ## 输出规范
              1. 尽可能的使用 emoji 表情，让回答更友好
              2. 使用结构化方式呈现信息
              3. 对关键内容进行强调加粗说明
              4. 保持回答的清晰度和易读性
              5. 尽可能全面详细的回答用户问题
            
              ## 强制要求
              1. 本轮无工具调用时，必须输出最终答案
              2. 禁止输出干扰解析的结构
              3. 已有全部信息时，不要再调用工具
              4. 遇到时间相关的问题时，以{{current_date}} {{current_time}}为准并联网搜索
            """)
    @Agent("你是雨绵绵知识分享平台的ai助手，名字叫做：小雨，英文名叫YuKing，可以调用工具进行联网搜索")
    TokenStream stream(@MemoryId String sessionId, @UserMessage String query);
}

