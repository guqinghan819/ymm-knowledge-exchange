package wcg.yhm.note.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.note.constants.MQConstant;
import wcg.yhm.note.constants.RedisKeyConstant;
import wcg.yhm.note.api.domain.event.MqLikeUnlikeDTO;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.domain.dataobject.NoteLike;
import wcg.yhm.note.domain.mapper.NoteLikeMapper;
import wcg.yhm.note.domain.mapper.NoteMapper;
import wcg.yhm.note.enums.*;
import wcg.yhm.note.model.dto.FindNoteDetailRequest;
import wcg.yhm.note.model.dto.NoteLikeRequest;

import wcg.yhm.note.model.dto.NoteUnlikeRequest;
import wcg.yhm.note.model.vo.FindNoteDetailResponse;
import wcg.yhm.note.service.INoteLikeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.note.service.INoteService;
import wcg.yhm.note.utils.CaffeineUtil;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * <p>
 * 笔记点赞表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
@Service
@Slf4j
public class NoteLikeServiceImpl extends ServiceImpl<NoteLikeMapper, NoteLike> implements INoteLikeService {
    @Resource
    private INoteService noteService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void noteLike(NoteLikeRequest noteLikeRequest) {
        Long noteId = noteLikeRequest.getNoteId();
        //判断笔记是否存在并获取创建者id
        Long creatorId = checkNoteIsExists(noteId);
        //判断用户是否已经点赞
        Long userId = UserContextHolder.getUserId();
        LocalDateTime operationTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_note_like_check.lua")));
        redisScript.setResultType(Long.class);
        String redisBloomKey = RedisKeyConstant.BLOOM_USER_NOTE_LIKE_LIST_KEY + userId;
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(redisBloomKey), noteId);
        LuaResultEnum resultEnum = LuaResultEnum.getEnumByCode(result);
        if (Objects.isNull(resultEnum)) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        switch (resultEnum) {
            case NOTE_ALREADY_LIKE -> {
                //布隆过滤器可能会发生误判进一步采取校验
                //判断zSet中是否有
                String zSetKey = RedisKeyConstant.ZSET_USER_NOTE_LIKE_LIKE_KEY + userId;
                Double score = redisTemplate.opsForZSet().score(zSetKey, noteId);
                if (Objects.nonNull(score)) {
                    throw new BizException(ErrorCodeEnum.NOTE_ALREADY_LIKE);
                }
                //查询数据库
                boolean exists = lambdaQuery().eq(NoteLike::getUserId, userId)
                        .eq(NoteLike::getNoteId, noteId)
                        .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus()).exists();
                if (exists) {
                    throw new BizException(ErrorCodeEnum.NOTE_ALREADY_LIKE);
                }
            }
            case BLOOM_NOT_EXISTS -> {
                //从数据库中检验是否已经点赞并初始化布隆过滤器
                long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                boolean exists = lambdaQuery().eq(NoteLike::getUserId, userId)
                        .eq(NoteLike::getNoteId, noteId)
                        .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus()).exists();
                if (exists) {
                    //存在就将该用户的点赞记录同步至redis中
                    taskExecutor.submit(() -> batchLike2Bloom(userId, redisBloomKey, ttl));
                    throw new BizException(ErrorCodeEnum.NOTE_ALREADY_LIKE);
                }
                //不存在就初始化布隆过滤器
                batchLike2Bloom(userId, redisBloomKey, ttl);
                DefaultRedisScript<Long> redisScript1 = new DefaultRedisScript<>();
                redisScript1.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_add_note_like.lua")));
                redisScript1.setResultType(Long.class);
                redisTemplate.execute(redisScript1, Collections.singletonList(redisBloomKey), noteId, ttl);
            }
        }

        //更新点赞zSet
        DefaultRedisScript<Long> zSetRedisScript = new DefaultRedisScript<>();
        zSetRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/zset_update_note_like.lua")));
        zSetRedisScript.setResultType(Long.class);
        long timeStamp = DateUtil.localDateTime2TimeStamp(operationTime);
        String zSetKey = RedisKeyConstant.ZSET_USER_NOTE_LIKE_LIKE_KEY + userId;
        Long result1 = redisTemplate.execute(zSetRedisScript, Collections.singletonList(zSetKey), noteId, timeStamp);
        long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        if (Objects.equals(result1, LuaResultEnum.ZSET_NOT_EXISTS.getCode())) {
            //zset为空就查询db同步数据
            List<NoteLike> noteLikes = lambdaQuery().eq(NoteLike::getUserId, userId)
                    .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus())
                    .orderByDesc(NoteLike::getCreateTime)
                    .list();
            DefaultRedisScript<Long> zSetRedisScript1 = new DefaultRedisScript<>();
            zSetRedisScript1.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/zset_batch_add_note_like.lua")));
            zSetRedisScript1.setResultType(Long.class);
            if (CollUtil.isNotEmpty(noteLikes)) {
                //构建参数
                Object[] luaArgs = buildLuaArgs(noteLikes, ttl);
                redisTemplate.execute(zSetRedisScript1, Collections.singletonList(zSetKey), luaArgs);
                //将当前点赞记录添加
                redisTemplate.execute(zSetRedisScript, Collections.singletonList(zSetKey), noteId, timeStamp);
            } else {
                //如果数据库为空，直接将当前记录添加到zset中
                List<Object> luaArgs = new ArrayList<>();
                luaArgs.add(timeStamp);
                luaArgs.add(noteId);
                luaArgs.add(ttl);
                redisTemplate.execute(zSetRedisScript1, Collections.singletonList(zSetKey), luaArgs.toArray());
            }
        }
        //mq 进行落库
        String operationId = UUID.randomUUID().toString();
        MqLikeUnlikeDTO mqLikeUnlikeDTO = new MqLikeUnlikeDTO();
        mqLikeUnlikeDTO.setOperationId(operationId);
        mqLikeUnlikeDTO.setType(NoteLikeEnum.LIKE.getStatus());
        mqLikeUnlikeDTO.setUserId(userId);
        mqLikeUnlikeDTO.setNoteId(noteId);
        mqLikeUnlikeDTO.setCreateTime(operationTime);
        mqLikeUnlikeDTO.setCreatorId(creatorId);
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(mqLikeUnlikeDTO))
                .setHeader(RocketMQHeaders.KEYS, operationId)
                .build();
        String destination = MQConstant.TOPIC_NOTE_LIKE_OR_UNLIKE + ":" + MQConstant.TAG_LIKE;
        String hashKey = String.valueOf(userId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=========> 点赞落库消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                redisTemplate.opsForZSet().remove(zSetKey, noteId);
                log.error("## 点赞落库消息发送失败, error: ", throwable);
            }
        });

    }

    @Override
    public void noteUnlike(NoteUnlikeRequest noteUnlikeRequest) {
        Long noteId = noteUnlikeRequest.getNoteId();
        Long userId = UserContextHolder.getUserId();
        //1.判断笔记是否存在
        Long creatorId = checkNoteIsExists(noteId);
        //2.判断笔记是否已经点赞
        String bloomKey = RedisKeyConstant.BLOOM_USER_NOTE_LIKE_LIST_KEY + userId;
        DefaultRedisScript<Long> bloomScript = new DefaultRedisScript<>();
        bloomScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_check_note_like.lua")));
        bloomScript.setResultType(Long.class);
        Long result = redisTemplate.execute(bloomScript, Collections.singletonList(bloomKey), noteId);
        BloomUnlikeEnum enumByCode = BloomUnlikeEnum.getEnumByCode(result);
        if (Objects.isNull(enumByCode)) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        switch(enumByCode) {
            case BLOOM_NOT_EXISTS -> {
                //过滤器中不存在，就查询db并同步数据
                boolean exists = lambdaQuery().eq(NoteLike::getUserId, userId)
                        .eq(NoteLike::getNoteId, noteId)
                        .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus()).exists();
                //判断是否点赞过
                if (!exists) {
                    throw new BizException(ErrorCodeEnum.UNLIKE_OPERATION_FAIL);
                }
                //异步同步数据
                long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                taskExecutor.submit(() -> batchLike2Bloom(userId, bloomKey, ttl));
            }
            case BLOOM_NOT_LIKE -> throw new BizException(ErrorCodeEnum.UNLIKE_OPERATION_FAIL);
        }

        //3.删除zset中的元素
        String zSetKey = RedisKeyConstant.ZSET_USER_NOTE_LIKE_LIKE_KEY + userId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(zSetKey))) {
            save2Zset(userId, zSetKey, 60 * 60 * 24L + RandomUtil.randomInt(60 * 60 * 24));
        }
        Double previousScore = redisTemplate.opsForZSet().score(zSetKey, noteId);
        redisTemplate.opsForZSet().remove(zSetKey, noteId);

        //发送落库消息
        String destination = MQConstant.TOPIC_NOTE_LIKE_OR_UNLIKE + ":" + MQConstant.TAG_UNLIKE;
        LocalDateTime operationTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String operationId = UUID.randomUUID().toString();
        MqLikeUnlikeDTO mqLikeUnlikeDTO = new MqLikeUnlikeDTO(
                operationId, NoteLikeEnum.UNLIKE.getStatus(), noteId, userId, operationTime, creatorId);
        String hashKey = String.valueOf(userId);
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(mqLikeUnlikeDTO))
                .setHeader(RocketMQHeaders.KEYS, operationId)
                .build();

        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==========> 取消点赞落库消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                if (previousScore != null) {
                    redisTemplate.opsForZSet().add(zSetKey, noteId, previousScore);
                }
                log.error("## 取消点赞落库消息发送失败, error: ", throwable);
            }
        });

    }


    /**
     * 构建lua参数
     * @param noteLikes 点赞记录
     * @return 参数
     */
    private Object[] buildLuaArgs(List<NoteLike> noteLikes, Long ttl) {
        int length = noteLikes.size() * 2 + 1;
        Object[] luaArgs = new Object[length];
        int i = 0;
        for (NoteLike noteLike : noteLikes) {
            luaArgs[i] = DateUtil.localDateTime2TimeStamp(noteLike.getCreateTime());
            luaArgs[i+1] = noteLike.getNoteId();
            i += 2;
        }
        luaArgs[length - 1] = ttl;
        return luaArgs;
    }

    private void save2Zset(Long userId, String zSetKey, long ttl) {
        List<NoteLike> noteLikes = lambdaQuery().eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus())
                .orderByDesc(NoteLike::getCreateTime)
                .list();
        if (CollUtil.isEmpty(noteLikes)) {
            return;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/zset_batch_add_note_like.lua")));
        script.setResultType(Long.class);
        redisTemplate.execute(script, Collections.singletonList(zSetKey), buildLuaArgs(noteLikes, ttl));
    }

    /**
     * 异步将点赞记录同步至布隆过滤器
     * @param userId 用户id
     * @param redisBloomKey key
     * @param ttl 过期时间
     */
    private void batchLike2Bloom(Long userId, String redisBloomKey, long ttl) {
            try {
                List<NoteLike> noteLikes = lambdaQuery().eq(NoteLike::getUserId, userId)
                        .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus()).list();
                if (CollUtil.isNotEmpty(noteLikes)) {
                    List<Object> luaArgs = new ArrayList<>();
                    //构建参数
                    noteLikes.forEach(noteLike -> luaArgs.add(noteLike.getNoteId()));
                    luaArgs.add(ttl);
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_batch_add_note_like.lua")));
                    redisScript.setResultType(Long.class);
                    redisTemplate.execute(redisScript, Collections.singletonList(redisBloomKey), luaArgs.toArray());
                }
            } catch (Exception e) {
                log.error("## 布隆过滤器初始化异常, e: ", e);
            }
    }

    /**
     * 校验笔记是否存在
     * @param noteId 笔记id
     * @return 创作者id
     */
    private Long checkNoteIsExists(Long noteId) {

        //本地缓存
        FindNoteDetailResponse noteDetail = CaffeineUtil.getNoteDetail(noteId);
        if (Objects.isNull(noteDetail)) {
            //从redis中读取
            String redisKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId;
            Object cachedValue = redisTemplate.opsForValue().get(redisKey);
            String jsonStr = cachedValue == null ? null : String.valueOf(cachedValue);
            FindNoteDetailResponse response = JsonUtil.parseObject(jsonStr, FindNoteDetailResponse.class);
            if (Objects.isNull(response)) {
                //从数据库中读取
                Note note = noteService.lambdaQuery().select(Note::getCreatorId).eq(Note::getId, noteId).one();
                if (Objects.isNull(note)) {
                    throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
                }
                //不为空就将数据进行同步
                taskExecutor.submit(() -> {
                    noteService.findNoteDetail(new FindNoteDetailRequest(noteId));
                });
                return note.getCreatorId();
            }
            return response.getCreatorId();
        }
        return noteDetail.getCreatorId();
    }
}
