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
import wcg.yhm.ymm.data.align.model.dto.MQCollectUnCollectDTO;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-17
 * @Description: 笔记收藏日增量消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT)
@Slf4j
public class TodayNoteCollectIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Value("${table.shards}")
    private int shards;
    @Resource
    private InsertRecordMapper insertRecordMapper;
    @Override
    public void onMessage(String s) {
        log.info("## ToDayNoteCollectIncrementData2DBConsumer 消费到了: {}", s);
        MQCollectUnCollectDTO mqCollectUnCollectDTO = JsonUtil.parseObject(s, MQCollectUnCollectDTO.class);
        if (Objects.isNull(mqCollectUnCollectDTO)) return;
        //判断bloom
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_today_note_collect_check.lua")));
        redisScript.setResultType(Long.class);
        Long creatorId = mqCollectUnCollectDTO.getCreatorId();
        Long noteId = mqCollectUnCollectDTO.getNoteId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String noteBloomKey = RedisKeyConstants.buildBloomUserNoteCollectNoteIdListKey(date);
        RedisScript<Long> redisScript1 = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(noteBloomKey), noteId);
        if (Objects.equals(result, 0L)) {
            //说明不存在，进行落库
            //根据分片数进行取模得到操作的表
            long noteIdHashKey = noteId % shards;

            try {
                insertRecordMapper
                        .insert2DataAlignNoteCollectCountTempTable(TableConstant.buildTableNameSuffix(date, noteIdHashKey), noteId);
            } catch (Exception e) {
                log.error("", e);
            }

            redisTemplate.execute(redisScript1, Collections.singletonList(noteBloomKey), noteId);
        }
        String creatorBloomKey = RedisKeyConstants.buildBloomUserNoteCollectUserIdListKey(date);
        result = redisTemplate.execute(redisScript, Collections.singletonList(creatorBloomKey), noteId);
        if (Objects.equals(result, 0L)) {
            long userIdHashKey = creatorId % shards;
            try {
                insertRecordMapper
                        .insert2DataAlignUserCollectCountTempTable(TableConstant.buildTableNameSuffix(date, userIdHashKey), creatorId);
            } catch (Exception e) {
                log.error("", e);
            }

            redisTemplate.execute(redisScript1, Collections.singletonList(creatorBloomKey), creatorId);
        }
    }
}
    
