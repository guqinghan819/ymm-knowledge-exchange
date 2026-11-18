package wcg.yhm.ai.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;

/**
 * <p>
 * AI PPT生成实例表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-11
 */
public interface IAiPptInstService extends IService<AiPptInst> {

    /**
     * 创建新的PPT实例
     *
     * @param conversationId 会话ID
     * @param query          用户原始需求
     * @return PPT实例
     */
    AiPptInst createInst(String conversationId, String query);
}
