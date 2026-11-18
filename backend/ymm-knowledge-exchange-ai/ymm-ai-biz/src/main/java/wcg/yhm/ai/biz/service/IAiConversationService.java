package wcg.yhm.ai.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.ai.biz.domain.pojo.AiConversation;

/**
 * <p>
 * AI对话会话表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-08
 */
public interface IAiConversationService extends IService<AiConversation> {

    /**
     * 创建会话
     */
    void create(String conversationId);

    /**
     * 删除当前用户的全部会话（软删除）
     */
    void deleteAll(Long userId);

    /**
     * 根据会话ID删除指定会话（软删除），校验归属
     */
    void deleteByConversationId(Long userId, String conversationId);
}
