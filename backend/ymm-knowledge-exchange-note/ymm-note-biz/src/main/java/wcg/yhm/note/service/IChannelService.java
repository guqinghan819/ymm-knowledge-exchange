package wcg.yhm.note.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.note.domain.dataobject.Channel;
import wcg.yhm.note.model.vo.ChannelAdminResponse;
import wcg.yhm.note.model.vo.ChannelResponse;

import java.util.List;

/**
 * <p>
 * 频道表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
public interface IChannelService extends IService<Channel> {
    /**
     * 获取频道
     * @return 频道列表
     */
    List<ChannelResponse> getChannel();

    /**
     * 管理端：查询全部频道(含绑定话题)
     * @return 频道管理列表
     */
    List<ChannelAdminResponse> adminChannelList();

    /**
     * 管理端：新增频道
     * @param name 频道名称
     */
    void createChannel(String name);

    /**
     * 管理端：修改频道名称
     * @param id 频道id
     * @param name 新名称
     */
    void updateChannel(Long id, String name);

    /**
     * 管理端：删除频道(逻辑删除，并清除该频道的话题绑定)
     * @param id 频道id
     */
    void deleteChannel(Long id);
}
