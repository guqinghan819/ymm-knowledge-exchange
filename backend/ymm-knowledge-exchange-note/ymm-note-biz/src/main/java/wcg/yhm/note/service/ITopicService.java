package wcg.yhm.note.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.note.domain.dataobject.Topic;
import wcg.yhm.note.model.dto.QueryTopicRequest;
import wcg.yhm.note.model.vo.TopicAdminResponse;
import wcg.yhm.note.model.vo.TopicResponse;

import java.util.List;

/**
 * <p>
 * 话题表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
public interface ITopicService extends IService<Topic> {

    /**
     * 通过频道id查询对应的话题
     * @return
     */
    List<TopicResponse> getTopicByChannelId(QueryTopicRequest queryTopicRequest);

    /**
     * 管理端：查询全部话题(含所属频道)
     * @return 话题管理列表
     */
    List<TopicAdminResponse> adminTopicList();

    /**
     * 管理端：新增话题
     * @param name 话题名称
     */
    void createTopic(String name);

    /**
     * 管理端：修改话题名称
     * @param id 话题id
     * @param name 新名称
     */
    void updateTopic(Long id, String name);

    /**
     * 管理端：删除话题(逻辑删除并清除绑定关系；若话题下存在笔记则拒绝)
     * @param id 话题id
     */
    void deleteTopic(Long id);
}
