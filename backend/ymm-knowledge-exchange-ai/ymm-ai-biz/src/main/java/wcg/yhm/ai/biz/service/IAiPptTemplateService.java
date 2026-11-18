package wcg.yhm.ai.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.ai.biz.domain.pojo.AiPptTemplate;

/**
 * <p>
 * AI PPT模板表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-11
 */
public interface IAiPptTemplateService extends IService<AiPptTemplate> {

    /**
     * 根据模板编码获取模板
     *
     * @param templateCode 模板编码
     * @return 模板
     */
    AiPptTemplate getByCode(String templateCode);
}
