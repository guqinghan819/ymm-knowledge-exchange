package wcg.yhm.note.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.checkerframework.checker.units.qual.C;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.note.api.domain.event.MQCollectUnCollectDTO;
import wcg.yhm.note.constants.MQConstant;
import wcg.yhm.note.constants.RedisKeyConstant;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.domain.dataobject.NoteCollection;
import wcg.yhm.note.domain.mapper.NoteCollectionMapper;
import wcg.yhm.note.enums.BloomNoteCollectEnum;
import wcg.yhm.note.enums.ErrorCodeEnum;
import wcg.yhm.note.enums.NoteCollectStatus;
import wcg.yhm.note.enums.ZsetNoteCollectEnum;
import wcg.yhm.note.model.dto.FindNoteDetailRequest;
import wcg.yhm.note.model.dto.NoteCollectRequest;
import wcg.yhm.note.model.dto.NoteUnCollectRequest;
import wcg.yhm.note.model.vo.FindNoteDetailResponse;
import wcg.yhm.note.service.INoteCollectionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.note.service.INoteService;
import wcg.yhm.note.utils.CaffeineUtil;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * 笔记收藏表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
@Service
@Slf4j
public class NoteCollectionServiceImpl extends ServiceImpl<NoteCollectionMapper, NoteCollection> implements INoteCollectionService {

    @Resource
    private INoteService noteService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void noteCollect(NoteCollectRequest noteCollectRequest) {
        Long noteId = noteCollectRequest.getNoteId();
        Long userId = UserContextHolder.getUserId();
        LocalDateTime operationTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        //笔记是否存在
        Long creatorId = checkNoteIsExists(noteId);
        //判断布隆过滤器中是否已经收藏
        DefaultRedisScript<Long> bloomScript1 = new DefaultRedisScript<>();
        bloomScript1.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_check_add_note_collect.lua")));
        bloomScript1.setResultType(Long.class);
        String bloomKey = RedisKeyConstant.BLOOM_USER_NOTE_COLLECT_LIST_KEY + userId;
        Long result = redisTemplate.execute(bloomScript1, Collections.singletonList(bloomKey), noteId);
        BloomNoteCollectEnum resultEnum = BloomNoteCollectEnum.getByCode(result);
        if (resultEnum == null) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        switch (resultEnum) {
            case BLOOM_NOTE_EXISTS -> {
                //如果bloom不存在就查询db并初始化bloom
                boolean exists = lambdaQuery().eq(NoteCollection::getUserId, userId)
                        .eq(NoteCollection::getNoteId, noteId)
                        .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode()).exists();
                long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                if (exists) {
                    //异步同步数据
                    taskExecutor.submit(() -> save2Bloom(userId, ttl));
                    //抛出已经收藏异常
                    throw new BizException(ErrorCodeEnum.NOTE_ALREADY_COLLECT);
                }
                //db中不存在就将该笔记添加到bloom中
                save2Bloom(userId, ttl); //将db中收藏的其他笔记同步
                DefaultRedisScript<Long> bloomScript2 = new DefaultRedisScript<>();
                bloomScript2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_add_note_collect.lua")));
                bloomScript2.setResultType(Long.class);
                redisTemplate.execute(bloomScript2, Collections.singletonList(bloomKey), noteId, ttl);
            }
            case BLOOM_NOTE_ALREADY_COLLECT -> {
                //如果bloom中已经收藏进一步判断zset
                String zSetKey = RedisKeyConstant.ZSET_USER_NOTE_COLLECT_KEY + userId;
                Double score = redisTemplate.opsForZSet().score(zSetKey, noteId);
                if (Objects.nonNull(score)) {
                    throw new BizException(ErrorCodeEnum.NOTE_ALREADY_COLLECT);
                }
                //如果zset中没有就查询db
                boolean exists = lambdaQuery().eq(NoteCollection::getUserId, userId)
                        .eq(NoteCollection::getNoteId, noteId)
                        .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode()).exists();
                if (exists) {
                    throw new BizException(ErrorCodeEnum.NOTE_ALREADY_COLLECT);
                }
            }
        }
        //bloom中添加成功后，更新zset
        String zsetKey = RedisKeyConstant.ZSET_USER_NOTE_COLLECT_KEY + userId;
        DefaultRedisScript<Long> zsetScript = new DefaultRedisScript<>();
        zsetScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/zset_update_note_collect.lua")));
        zsetScript.setResultType(Long.class);
        Long result1 = redisTemplate
                .execute(zsetScript, Collections.singletonList(zsetKey),
                        DateUtil.localDateTime2TimeStamp(operationTime), noteId);
        if (Objects.equals(result1, ZsetNoteCollectEnum.ZSET_NOT_EXISTS.getCode())) {
            long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            //当zset不存在时进行初始化操作
            save2Zset(userId, zsetKey, ttl, noteId);
        }
        //发送落库消息
        String destination = MQConstant.TOPIC_NOTE_COLLECT_OR_UN_COLLECT + ":" + MQConstant.TAG_COLLECT;
        String operationId = UUID.randomUUID().toString();
        MQCollectUnCollectDTO mqCollectUnCollectDTO = new MQCollectUnCollectDTO(
                operationId, userId, noteId, NoteCollectStatus.COLLECT.getCode(), operationTime, creatorId);
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(mqCollectUnCollectDTO))
                .setHeader(RocketMQHeaders.KEYS, operationId)
                .build();
        String hashKey = String.valueOf(userId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=========> 收藏落库消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                redisTemplate.opsForZSet().remove(zsetKey, noteId);
                log.error("=========> 收藏落库消息发送失败, error: ", throwable);
            }
        });
    }

    @Override
    public void noteUnCollect(NoteUnCollectRequest noteUnCollectRequest) {
        Long noteId = noteUnCollectRequest.getNoteId();
        Long creatorId = checkNoteIsExists(noteId);
        Long userId = UserContextHolder.getUserId();
        //判断bloom是否收藏，收藏了才能取消
        String bloomKey = RedisKeyConstant.BLOOM_USER_NOTE_COLLECT_LIST_KEY + userId;
        DefaultRedisScript<Long> bloomScript = new DefaultRedisScript<>();
        bloomScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_check_note_collect.lua")));
        bloomScript.setResultType(Long.class);
        Long result = redisTemplate.execute(bloomScript, Collections.singletonList(bloomKey), noteId);
        BloomNoteCollectEnum collectEnum = BloomNoteCollectEnum.getByCode(result);
        if (Objects.isNull(collectEnum)) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        switch (collectEnum) {
            case BLOOM_NOTE_EXISTS -> {
                //bloom不存在就初始化
                boolean exists = lambdaQuery().eq(NoteCollection::getUserId, userId)
                        .eq(NoteCollection::getNoteId, noteId)
                        .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode()).exists();
                if (!exists) {
                    throw new BizException(ErrorCodeEnum.UN_COLLECT_OPERATION_FAIL);
                }
                //收藏记录存在就异步将数据同步至bloom中
                long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                taskExecutor.submit(() -> save2Bloom(userId, ttl));
            }
            case BLOOM_NOTE_NOT_COLLECT -> throw new BizException(ErrorCodeEnum.UN_COLLECT_OPERATION_FAIL);
        }
        //删除zset中的记录
        String zsetKey = RedisKeyConstant.ZSET_USER_NOTE_COLLECT_KEY + userId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(zsetKey))) {
            save2Zset(userId, zsetKey, 60 * 60 * 24L + RandomUtil.randomInt(60 * 60 * 24), noteId);
        }
        Double previousScore = redisTemplate.opsForZSet().score(zsetKey, noteId);
        redisTemplate.opsForZSet().remove(zsetKey, noteId);
        //发送落库消息
        String destination = MQConstant.TOPIC_NOTE_COLLECT_OR_UN_COLLECT + ":" +MQConstant.TAG_UN_COLLECT;
        LocalDateTime operationTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String operationId = UUID.randomUUID().toString();
        MQCollectUnCollectDTO mqCollectUnCollectDTO = new MQCollectUnCollectDTO(
                operationId, userId, noteId, NoteCollectStatus.NOT_COLLECT.getCode(), operationTime, creatorId);
        String hashKey = String.valueOf(userId);
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(mqCollectUnCollectDTO))
                .setHeader(RocketMQHeaders.KEYS, operationId)
                .build();
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("========> 发送取消收藏落库消息成功 result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                if (previousScore != null) {
                    redisTemplate.opsForZSet().add(zsetKey, noteId, previousScore);
                }
                log.error("========> 发送取消收藏落库消息失败 error: ", throwable);
            }
        });
    }

    /**
     * 将收藏记录同步到zset中
     * @param userId 用户id
     * @param zsetKey 键
     * @param ttl 过期时间
     * @param noteId 笔记id
     */
    private void save2Zset(Long userId, String zsetKey, long ttl, Long noteId) {
        try {
            List<NoteCollection> list = lambdaQuery().eq(NoteCollection::getUserId, userId)
                    .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode())
                    .orderByDesc(NoteCollection::getCreateTime)
                    .list();
            DefaultRedisScript<Long> zsetScript = new DefaultRedisScript<>();
            zsetScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/zset_batch_add_note_collect.lua")));
            zsetScript.setResultType(Long.class);
            if (CollUtil.isNotEmpty(list)) {
                //构建参数
                Object[] luaArgs = buildLuaArgs(list, ttl);
                redisTemplate.execute(zsetScript, Collections.singletonList(zsetKey), luaArgs);
                //将这条记录添加
                DefaultRedisScript<Long> zsetScript1 = new DefaultRedisScript<>();
                zsetScript1.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/zset_update_note_collect.lua")));
                zsetScript1.setResultType(Long.class);
                redisTemplate.execute(zsetScript1, Collections.singletonList(zsetKey),
                        DateUtil.localDateTime2TimeStamp(LocalDateTime.now()), noteId);
            } else {
                //如果数据库中没有数据，直接将这条数据插入zset
                List<Object> args = new ArrayList<>();
                args.add(DateUtil.localDateTime2TimeStamp(LocalDateTime.now()));
                args.add(noteId);
                args.add(ttl);
                redisTemplate.execute(zsetScript, Collections.singletonList(zsetKey), args.toArray());
            }
        } catch (Exception e) {
            log.error("## zset同步用户收藏笔记数据失败, error: ", e);
        }
    }


    /**
     * 将收藏记录同步到bloom中
     * @param userId 用户id
     * @param ttl 过期时间
     */
    private void save2Bloom(Long userId,  long ttl) {
        try {
            List<NoteCollection> list = lambdaQuery().eq(NoteCollection::getUserId, userId)
                    .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode())
                    .list();
            if (CollUtil.isNotEmpty(list)) {
                //构建脚本参数
                String bloomKey = RedisKeyConstant.BLOOM_USER_NOTE_COLLECT_LIST_KEY + userId;
                List<Object> luaArgs = new ArrayList<>();
                list.forEach(n -> luaArgs.add(n.getNoteId()));
                luaArgs.add(ttl);
                DefaultRedisScript<Long> bloomScript = new DefaultRedisScript<>();
                bloomScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_batch_add_note_collect.lua")));
                bloomScript.setResultType(Long.class);
                redisTemplate.execute(bloomScript, Collections.singletonList(bloomKey), luaArgs.toArray());
            }
        } catch (Exception e) {
            log.error("## bloom同步用户收藏笔记数据失败, error: ", e);
        }
    }


    /**
     * 构建脚本参数
     * @param list 数据列表
     * @param ttl 过期时间
     * @return 脚本参数
     */
    private Object[] buildLuaArgs(List<NoteCollection> list, long ttl) {
        int length = list.size() * 2 + 1;
        Object[] luaArgs = new Object[length];
        int i = 0;
        for (NoteCollection noteCollection : list) {
            luaArgs[i] = DateUtil.localDateTime2TimeStamp(noteCollection.getCreateTime());
            luaArgs[i + 1] = noteCollection.getNoteId();
            i += 2;
        }
        luaArgs[length - 1] = ttl;
        return luaArgs;
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
