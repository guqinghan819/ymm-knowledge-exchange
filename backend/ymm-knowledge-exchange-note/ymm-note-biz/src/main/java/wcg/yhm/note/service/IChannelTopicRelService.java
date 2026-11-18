package wcg.yhm.note.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.note.domain.dataobject.ChannelTopicRel;

import java.util.List;

/**
 * <p>
 * 频道-话题关联表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
public interface IChannelTopicRelService extends IService<ChannelTopicRel> {

    /**
     * 管理端：设置频道下绑定的话题(覆盖式绑定，先删后插)
     * @param channelId 频道id
     * @param topicIds 话题id列表(为空表示清空该频道的绑定)
     */
    void bindTopics(Long channelId, List<Long> topicIds);
}
