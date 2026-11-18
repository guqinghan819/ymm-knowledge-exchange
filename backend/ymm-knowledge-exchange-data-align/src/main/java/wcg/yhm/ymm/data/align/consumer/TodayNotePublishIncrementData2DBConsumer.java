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
import wcg.yhm.ymm.data.align.model.dto.MQPublishCountDTO;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-17
 * @Description:
 * @Version: 1.0
 */

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_data_align_" + MQConstants.TOPIC_COUNT_NOTE_PUBLISH,
topic = MQConstants.TOPIC_COUNT_NOTE_PUBLISH)
public class TodayNotePublishIncrementData2DBConsumer implements RocketMQListener<String> {
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
        log.info("## TodayNotePublishIncrementData2DBConsumer 消费到了 MQ: {}", s);
        MQPublishCountDTO mqPublishCountDTO = JsonUtil.parseObject(s, MQPublishCountDTO.class);
        if (Objects.isNull(mqPublishCountDTO)) return;
        //判断bloom
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_today_note_publish_check.lua")));
        redisScript.setResultType(Long.class);
        Long userId = mqPublishCountDTO.getUserId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = RedisKeyConstants.buildBloomDataAlignUserNotePublishKey(date);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(redisKey), userId);
        // 若布隆过滤器判断不存在（绝对正确）
        if (Objects.equals(result, 0L)) {
            // 2. 若无，才会落库，减轻数据库压力

            // 根据分片总数，取模，分别获取对应的分片序号
            long userIdHashKey = userId % shards;

            // 将日增量变更数据，写入日增量表中
            // - t_data_align_note_publish_count_temp_日期_分片序号
            insertRecordMapper.insert2DataAlignUserNotePublishCountTempTable(TableConstant.buildTableNameSuffix(date, userIdHashKey), userId);

            RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
            redisTemplate.execute(bloomAddScript, Collections.singletonList(redisKey), userId);

        }
    }
}
    
