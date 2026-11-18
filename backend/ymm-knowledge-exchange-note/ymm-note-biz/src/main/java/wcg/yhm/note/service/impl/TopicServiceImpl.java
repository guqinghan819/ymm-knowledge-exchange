package wcg.yhm.note.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.note.constants.RedisKeyConstant;
import wcg.yhm.note.domain.dataobject.Channel;
import wcg.yhm.note.domain.dataobject.ChannelTopicRel;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.domain.dataobject.Topic;
import wcg.yhm.note.domain.mapper.ChannelMapper;
import wcg.yhm.note.domain.mapper.NoteMapper;
import wcg.yhm.note.domain.mapper.TopicMapper;
import wcg.yhm.note.enums.DeleteEnum;
import wcg.yhm.note.enums.ErrorCodeEnum;
import wcg.yhm.note.enums.NoteStatusEnum;
import wcg.yhm.note.model.dto.QueryTopicRequest;
import wcg.yhm.note.model.vo.ChannelResponse;
import wcg.yhm.note.model.vo.TopicAdminResponse;
import wcg.yhm.note.model.vo.TopicResponse;
import wcg.yhm.note.service.IChannelService;
import wcg.yhm.note.service.IChannelTopicRelService;
import wcg.yhm.note.service.ITopicService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
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
 * 话题表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements ITopicService {
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private IChannelTopicRelService channelTopicRelService;
    @Resource
    private IChannelService channelService;
    @Resource
    private ChannelMapper channelMapper;
    @Resource
    private NoteMapper noteMapper;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Override
    public List<TopicResponse> getTopicByChannelId(QueryTopicRequest queryTopicRequest) {
        Long channelId = queryTopicRequest.getId();
        //查询redis
        String redisKey = RedisKeyConstant.TOPIC_CHANNEL_KEY_PREFIX + channelId;
        String topicStr = redisTemplate.opsForValue().get(redisKey);
        List<TopicResponse> topicResponses = JsonUtil.str2List(topicStr, TopicResponse.class);
        if (CollUtil.isNotEmpty(topicResponses)) {
            return topicResponses;
        }

        //判断channelId是否存在
        boolean exists = channelService.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getIsDeleted, DeleteEnum.NO.isDelete()).exists();
        long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        if (!exists) {
            //缓存空值，设置随机ttl
            redisTemplate.opsForValue().set(redisKey, "", ttl, TimeUnit.SECONDS);
            throw new BizException(ErrorCodeEnum.CHANNEL_NO_EXISTS);
        }
        //查询db(频道下对应的话题id)
        List<ChannelTopicRel> list = channelTopicRelService.lambdaQuery()
                .select(ChannelTopicRel::getTopicId)
                .eq(ChannelTopicRel::getChannelId, channelId).list();
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        //根据话题id查询对应的话题
        List<Long> topicIds = list.stream().map(ChannelTopicRel::getTopicId).toList();
        List<Topic> topicList = lambdaQuery().in(Topic::getId, topicIds)
                .eq(Topic::getIsDeleted, DeleteEnum.NO.isDelete()).list();
        //封装
        List<TopicResponse> topicResponseList = topicList
                .stream().map(t -> BeanUtil.copyProperties(t, TopicResponse.class))
                .collect(Collectors.toList());
        //异步重建redis
        taskExecutor.submit(() -> {
            String topicJsonStr = JsonUtil.toJsonString(topicResponseList);
            redisTemplate.opsForValue().set(redisKey, topicJsonStr, ttl, TimeUnit.SECONDS);
        });
        return topicResponseList;
    }

    @Override
    public List<TopicAdminResponse> adminTopicList() {
        List<Topic> topicList = lambdaQuery()
                .eq(Topic::getIsDeleted, DeleteEnum.NO.isDelete())
                .orderByAsc(Topic::getId)
                .list();
        if (CollUtil.isEmpty(topicList)) {
            return new ArrayList<>();
        }
        List<Long> topicIds = topicList.stream().map(Topic::getId).toList();
        //查询全部绑定关系，按话题分组
        List<ChannelTopicRel> relList = channelTopicRelService.lambdaQuery()
                .in(ChannelTopicRel::getTopicId, topicIds).list();
        Map<Long, List<Long>> topicIdWithChannelIds = relList.stream()
                .collect(Collectors.groupingBy(ChannelTopicRel::getTopicId,
                        Collectors.mapping(ChannelTopicRel::getChannelId, Collectors.toList())));
        //查询全部频道
        List<Long> channelIds = relList.stream().map(ChannelTopicRel::getChannelId).distinct().toList();
        Map<Long, ChannelResponse> channelMap = new HashMap<>();
        if (CollUtil.isNotEmpty(channelIds)) {
            List<Channel> channelList = channelMapper.selectBatchIds(channelIds);
            channelMap = channelList.stream()
                    .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                    .collect(Collectors.toMap(Channel::getId, c -> BeanUtil.copyProperties(c, ChannelResponse.class)));
        }
        Map<Long, ChannelResponse> finalChannelMap = channelMap;
        return topicList.stream().map(topic -> {
            TopicAdminResponse response = BeanUtil.copyProperties(topic, TopicAdminResponse.class);
            List<Long> ids = topicIdWithChannelIds.getOrDefault(topic.getId(), List.of());
            List<ChannelResponse> channels = ids.stream()
                    .map(finalChannelMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            response.setChannels(channels);
            return response;
        }).toList();
    }

    @Override
    public void createTopic(String name) {
        if (StrUtil.isBlank(name)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //名称唯一校验
        boolean exists = lambdaQuery().eq(Topic::getName, name)
                .eq(Topic::getIsDeleted, DeleteEnum.NO.isDelete()).exists();
        if (exists) {
            throw new BizException(ErrorCodeEnum.TOPIC_NAME_EXISTS);
        }
        Topic topic = new Topic();
        topic.setName(name);
        topic.setCreateTime(LocalDateTime.now());
        topic.setUpdateTime(LocalDateTime.now());
        topic.setIsDeleted(DeleteEnum.NO.isDelete());
        boolean result = save(topic);
        if (!result) {
            throw new BizException(ErrorCodeEnum.TOPIC_CREATE_FAIL);
        }
    }

    @Override
    public void updateTopic(Long id, String name) {
        if (Objects.isNull(id) || StrUtil.isBlank(name)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Topic topic = getById(id);
        if (Objects.isNull(topic) || Boolean.TRUE.equals(topic.getIsDeleted())) {
            throw new BizException(ErrorCodeEnum.TOPIC_NOT_FOUND);
        }
        //名称唯一校验(排除自己)
        boolean exists = lambdaQuery().eq(Topic::getName, name)
                .eq(Topic::getIsDeleted, DeleteEnum.NO.isDelete())
                .ne(Topic::getId, id).exists();
        if (exists) {
            throw new BizException(ErrorCodeEnum.TOPIC_NAME_EXISTS);
        }
        Topic update = new Topic();
        update.setId(id);
        update.setName(name);
        update.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(update);
        if (!result) {
            throw new BizException(ErrorCodeEnum.TOPIC_UPDATE_FAIL);
        }
        //话题名称变更后，清除所有包含该话题的频道的缓存
        clearTopicChannelCacheByTopicId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTopic(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Topic topic = getById(id);
        if (Objects.isNull(topic) || Boolean.TRUE.equals(topic.getIsDeleted())) {
            throw new BizException(ErrorCodeEnum.TOPIC_NOT_FOUND);
        }
        //话题下存在未删除的笔记时禁止删除
        Long noteCount = noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getTopicId, id)
                .ne(Note::getStatus, NoteStatusEnum.DELETED.getValue()));
        if (noteCount != null && noteCount > 0) {
            throw new BizException(ErrorCodeEnum.TOPIC_HAS_NOTES);
        }
        //先取出该话题涉及到的频道id(用于清除缓存)
        List<Long> channelIds = channelTopicRelService.lambdaQuery()
                .select(ChannelTopicRel::getChannelId)
                .eq(ChannelTopicRel::getTopicId, id).list()
                .stream().map(ChannelTopicRel::getChannelId).distinct().toList();
        //逻辑删除话题
        Topic update = new Topic();
        update.setId(id);
        update.setIsDeleted(DeleteEnum.YES.isDelete());
        update.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(update);
        if (!result) {
            throw new BizException(ErrorCodeEnum.TOPIC_DELETE_FAIL);
        }
        //清除该话题的绑定关系
        channelTopicRelService.lambdaUpdate()
                .eq(ChannelTopicRel::getTopicId, id)
                .remove();
        //清除相关频道的话题缓存
        channelIds.forEach(channelId -> redisTemplate.delete(RedisKeyConstant.TOPIC_CHANNEL_KEY_PREFIX + channelId));
    }

    /**
     * 根据话题id清除其所属频道的redis话题缓存
     * @param topicId 话题id
     */
    private void clearTopicChannelCacheByTopicId(Long topicId) {
        List<Long> channelIds = channelTopicRelService.lambdaQuery()
                .select(ChannelTopicRel::getChannelId)
                .eq(ChannelTopicRel::getTopicId, topicId).list()
                .stream().map(ChannelTopicRel::getChannelId).distinct().toList();
        channelIds.forEach(channelId -> redisTemplate.delete(RedisKeyConstant.TOPIC_CHANNEL_KEY_PREFIX + channelId));
    }
}
