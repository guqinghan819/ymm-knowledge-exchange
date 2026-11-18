package wcg.yhm.note.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.note.constants.RedisKeyConstant;
import wcg.yhm.note.domain.dataobject.Channel;
import wcg.yhm.note.domain.dataobject.ChannelTopicRel;
import wcg.yhm.note.domain.dataobject.Topic;
import wcg.yhm.note.domain.mapper.ChannelMapper;
import wcg.yhm.note.domain.mapper.TopicMapper;
import wcg.yhm.note.enums.DeleteEnum;
import wcg.yhm.note.enums.ErrorCodeEnum;
import wcg.yhm.note.model.vo.ChannelAdminResponse;
import wcg.yhm.note.model.vo.ChannelResponse;
import wcg.yhm.note.model.vo.TopicResponse;
import wcg.yhm.note.service.IChannelService;
import wcg.yhm.note.service.IChannelTopicRelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.note.utils.CaffeineUtil;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 频道表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@Service
public class ChannelServiceImpl extends ServiceImpl<ChannelMapper, Channel> implements IChannelService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Resource
    private IChannelTopicRelService channelTopicRelService;

    @Resource
    private TopicMapper topicMapper;

    @Override
    public List<ChannelResponse> getChannel() {
        //从本地缓存获取
        List<ChannelResponse> channelCache = CaffeineUtil.getChannelCache();
        if (CollUtil.isNotEmpty(channelCache)) {
            //直接返回
            return channelCache;
        }
        //查询redis
        String channelJsonStr = redisTemplate.opsForValue().get(RedisKeyConstant.CHANNEL_KEY);
        //str -> list
        channelCache = JsonUtil.str2List(channelJsonStr, ChannelResponse.class);
        if (CollUtil.isNotEmpty(channelCache)) {
            //重建本地缓存
            CaffeineUtil.setChannelCache(channelCache);
            return channelCache;
        }
        //缓存中没有就查询db并异步重建redis
        List<Channel> channelList =
                lambdaQuery().eq(Channel::getIsDeleted, DeleteEnum.NO.isDelete()).list();
        List<ChannelResponse> finalChannelCache =
                channelList.stream()
                        .map(c -> BeanUtil.copyProperties(c, ChannelResponse.class))
                        .collect(Collectors.toList());
        taskExecutor.submit(() -> {
            long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            String jsonString = JsonUtil.toJsonString(finalChannelCache);
            redisTemplate.opsForValue().set(RedisKeyConstant.CHANNEL_KEY, jsonString, ttl, TimeUnit.SECONDS);

        });
        return finalChannelCache;
    }

    @Override
    public List<ChannelAdminResponse> adminChannelList() {
        List<Channel> channelList = lambdaQuery()
                .eq(Channel::getIsDeleted, DeleteEnum.NO.isDelete())
                .orderByAsc(Channel::getId)
                .list();
        if (CollUtil.isEmpty(channelList)) {
            return new ArrayList<>();
        }
        List<Long> channelIds = channelList.stream().map(Channel::getId).toList();
        //查询全部绑定关系，按频道分组
        List<ChannelTopicRel> relList = channelTopicRelService.lambdaQuery()
                .in(ChannelTopicRel::getChannelId, channelIds).list();
        Map<Long, List<Long>> channelIdWithTopicIds = relList.stream()
                .collect(Collectors.groupingBy(ChannelTopicRel::getChannelId,
                        Collectors.mapping(ChannelTopicRel::getTopicId, Collectors.toList())));
        //查询全部话题
        List<Long> topicIds = relList.stream().map(ChannelTopicRel::getTopicId).distinct().toList();
        Map<Long, TopicResponse> topicMap = new HashMap<>();
        if (CollUtil.isNotEmpty(topicIds)) {
            List<Topic> topicList = topicMapper.selectBatchIds(topicIds);
            topicMap = topicList.stream()
                    .filter(t -> Boolean.FALSE.equals(t.getIsDeleted()))
                    .collect(Collectors.toMap(Topic::getId, t -> BeanUtil.copyProperties(t, TopicResponse.class)));
        }
        Map<Long, TopicResponse> finalTopicMap = topicMap;
        return channelList.stream().map(channel -> {
            ChannelAdminResponse response = BeanUtil.copyProperties(channel, ChannelAdminResponse.class);
            List<Long> ids = channelIdWithTopicIds.getOrDefault(channel.getId(), List.of());
            List<TopicResponse> topics = ids.stream()
                    .map(finalTopicMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            response.setTopics(topics);
            return response;
        }).toList();
    }

    @Override
    public void createChannel(String name) {
        if (StrUtil.isBlank(name)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //名称唯一校验
        boolean exists = lambdaQuery().eq(Channel::getName, name)
                .eq(Channel::getIsDeleted, DeleteEnum.NO.isDelete()).exists();
        if (exists) {
            throw new BizException(ErrorCodeEnum.CHANNEL_NAME_EXISTS);
        }
        Channel channel = new Channel();
        channel.setName(name);
        channel.setCreateTime(LocalDateTime.now());
        channel.setUpdateTime(LocalDateTime.now());
        channel.setIsDeleted(DeleteEnum.NO.isDelete());
        boolean result = save(channel);
        if (!result) {
            throw new BizException(ErrorCodeEnum.CHANNEL_CREATE_FAIL);
        }
        clearChannelCache();
    }

    @Override
    public void updateChannel(Long id, String name) {
        if (Objects.isNull(id) || StrUtil.isBlank(name)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Channel channel = getById(id);
        if (Objects.isNull(channel) || Boolean.TRUE.equals(channel.getIsDeleted())) {
            throw new BizException(ErrorCodeEnum.CHANNEL_NO_EXISTS);
        }
        //名称唯一校验(排除自己)
        boolean exists = lambdaQuery().eq(Channel::getName, name)
                .eq(Channel::getIsDeleted, DeleteEnum.NO.isDelete())
                .ne(Channel::getId, id).exists();
        if (exists) {
            throw new BizException(ErrorCodeEnum.CHANNEL_NAME_EXISTS);
        }
        Channel update = new Channel();
        update.setId(id);
        update.setName(name);
        update.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(update);
        if (!result) {
            throw new BizException(ErrorCodeEnum.CHANNEL_UPDATE_FAIL);
        }
        clearChannelCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Channel channel = getById(id);
        if (Objects.isNull(channel) || Boolean.TRUE.equals(channel.getIsDeleted())) {
            throw new BizException(ErrorCodeEnum.CHANNEL_NO_EXISTS);
        }
        //逻辑删除频道
        Channel update = new Channel();
        update.setId(id);
        update.setIsDeleted(DeleteEnum.YES.isDelete());
        update.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(update);
        if (!result) {
            throw new BizException(ErrorCodeEnum.CHANNEL_DELETE_FAIL);
        }
        //清除该频道下的话题绑定
        channelTopicRelService.lambdaUpdate()
                .eq(ChannelTopicRel::getChannelId, id)
                .remove();
        clearChannelCache();
        //清除该频道的话题缓存
        redisTemplate.delete(RedisKeyConstant.TOPIC_CHANNEL_KEY_PREFIX + id);
    }

    /**
     * 清除频道缓存(redis + 本地缓存)
     */
    private void clearChannelCache() {
        redisTemplate.delete(RedisKeyConstant.CHANNEL_KEY);
        CaffeineUtil.clearChannelCache();
    }
}
