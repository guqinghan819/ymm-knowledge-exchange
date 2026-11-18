package wcg.yhm.note.service.impl;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.note.constants.RedisKeyConstant;
import wcg.yhm.note.domain.dataobject.Channel;
import wcg.yhm.note.domain.dataobject.ChannelTopicRel;
import wcg.yhm.note.domain.dataobject.Topic;
import wcg.yhm.note.domain.mapper.ChannelMapper;
import wcg.yhm.note.domain.mapper.ChannelTopicRelMapper;
import wcg.yhm.note.domain.mapper.TopicMapper;
import wcg.yhm.note.enums.ErrorCodeEnum;
import wcg.yhm.note.service.IChannelTopicRelService;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 频道-话题关联表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@Service
public class ChannelTopicRelServiceImpl extends ServiceImpl<ChannelTopicRelMapper, ChannelTopicRel> implements IChannelTopicRelService {

    @Resource
    private ChannelMapper channelMapper;

    @Resource
    private TopicMapper topicMapper;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindTopics(Long channelId, List<Long> topicIds) {
        if (Objects.isNull(channelId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //校验频道存在
        Channel channel = channelMapper.selectById(channelId);
        if (Objects.isNull(channel) || Boolean.TRUE.equals(channel.getIsDeleted())) {
            throw new BizException(ErrorCodeEnum.CHANNEL_NO_EXISTS);
        }
        //过滤出有效的话题id
        List<Long> validTopicIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(topicIds)) {
            List<Topic> topicList = topicMapper.selectBatchIds(topicIds);
            Set<Long> validIds = topicList.stream()
                    .filter(t -> Boolean.FALSE.equals(t.getIsDeleted()))
                    .map(Topic::getId)
                    .collect(Collectors.toSet());
            validTopicIds = topicIds.stream().filter(validIds::contains).distinct().toList();
        }
        //覆盖式绑定：先删除该频道的原有绑定，再插入新绑定
        lambdaUpdate().eq(ChannelTopicRel::getChannelId, channelId).remove();
        if (CollUtil.isNotEmpty(validTopicIds)) {
            List<ChannelTopicRel> relList = validTopicIds.stream().map(topicId -> {
                ChannelTopicRel rel = new ChannelTopicRel();
                rel.setChannelId(channelId);
                rel.setTopicId(topicId);
                rel.setCreateTime(LocalDateTime.now());
                rel.setUpdateTime(LocalDateTime.now());
                return rel;
            }).toList();
            boolean inserted = saveBatch(relList);
            if (!inserted) {
                throw new BizException(ErrorCodeEnum.CHANNEL_TOPIC_BIND_FAIL);
            }
        }
        //清除该频道的话题缓存
        redisTemplate.delete(RedisKeyConstant.TOPIC_CHANNEL_KEY_PREFIX + channelId);
    }
}
