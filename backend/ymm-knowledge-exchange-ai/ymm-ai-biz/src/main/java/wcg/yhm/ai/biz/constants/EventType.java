package wcg.yhm.ai.biz.constants;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-07
 * @Description: 时间类型
 * @Version: 1.0
 */

public interface EventType {
    /**
     * 回答文本
     */
    String TYPE_TEXT = "text";
    /**
     * 思考
     */
    String TYPE_THINKING = "thinking";
    /**
     * 参考引用
     */
    String TYPE_REFERENCE = "reference";
    /**
     * 错误
     */
    String TYPE_ERROR = "error";
    /**
     * 执行步骤
     */
    String TYPE_STEP = "step";
    /**
     *推荐问题
     */
    String TYPE_RECOMMEND = "recommend";
}
    
