package wcg.yhm.ymm.data.align.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import wcg.yhm.ymm.data.align.constants.MQConstants;
import wcg.yhm.ymm.data.align.constants.RedisKeyConstants;
import wcg.yhm.ymm.data.align.constants.TableConstant;
import wcg.yhm.ymm.data.align.mapper.InsertRecordMapper;
import wcg.yhm.ymm.data.align.model.dto.MqLikeUnlikeDTO;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-17
 * @Description: 笔记点赞日增量计数落库消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_LIKE,
topic = MQConstants.TOPIC_COUNT_NOTE_LIKE)
@Slf4j
public class TodayNoteLikeIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private InsertRecordMapper insertRecordMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Value("${table.shards}")
    private int shards;
    @Override
    public void onMessage(String s) {
        log.info("## ToDayNoteLikeIncrementData2DBConsumer 消费到了: {}", s);
        MqLikeUnlikeDTO mqLikeUnlikeDTO = JsonUtil.parseObject(s, MqLikeUnlikeDTO.class);
        if (Objects.isNull(mqLikeUnlikeDTO)) return;
        Long noteId = mqLikeUnlikeDTO.getNoteId();
        Long creatorId = mqLikeUnlikeDTO.getCreatorId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String noteBloomKey = RedisKeyConstants.buildBloomUserNoteLikeNoteIdListKey(date);
        //1. bloom判断日增点赞记录是否已经存在
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_today_note_like_check.lua")));
        redisScript.setResultType(Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(noteBloomKey), noteId);
        RedisScript<Long> addRedisScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
        //2. 不存在就落库
        if (Objects.equals(result, 0L)) {
            //根据分片数进行取模得到操作的表
            long noteIdHashKey = noteId % shards;
            String tableNameSuffix = TableConstant.buildTableNameSuffix(date, noteIdHashKey);
            try {
                insertRecordMapper.insert2DataAlignNoteLikeCountTempTable(tableNameSuffix, noteId);
            } catch (Exception e) {
                log.error("", e);
            }
            //3.落库成功后添加到bloom中
            redisTemplate.execute(addRedisScript, Collections.singletonList(noteBloomKey), noteId);
        }
        String creatorBloomKey = RedisKeyConstants.buildBloomUserNoteLikeUserIdListKey(date);
        result = redisTemplate.execute(redisScript, Collections.singletonList(creatorBloomKey), creatorId);
        if (Objects.equals(result, 0L)) {
            long userIdHashKey = creatorId % shards;
            String tableNameSuffix = TableConstant.buildTableNameSuffix(date, userIdHashKey);
            try {
                insertRecordMapper.insert2DataAlignUserLikeCountTempTable(tableNameSuffix, creatorId);
            } catch (Exception e) {
                log.error("", e);
            }
            redisTemplate.execute(addRedisScript, Collections.singletonList(creatorBloomKey), creatorId);
        }
    }
}
    
