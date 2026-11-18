package wcg.yhm.ai.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import wcg.yhm.ai.biz.domain.pojo.AiPptTemplate;
import wcg.yhm.ai.biz.mapper.AiPptTemplateMapper;
import wcg.yhm.ai.biz.service.IAiPptTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * AI PPT模板表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-11
 */
@Service
public class AiPptTemplateServiceImpl extends ServiceImpl<AiPptTemplateMapper, AiPptTemplate> implements IAiPptTemplateService {

    @Override
    public AiPptTemplate getByCode(String templateCode) {
        LambdaQueryWrapper<AiPptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPptTemplate::getTemplateCode, templateCode);
        return getOne(wrapper);
    }
}
