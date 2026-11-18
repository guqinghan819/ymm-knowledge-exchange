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
import wcg.yhm.ymm.data.align.model.dto.CountFollowUnfollowDTO;
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
@RocketMQMessageListener(consumerGroup = "ymm_group_data_align_" + MQConstants.TOPIC_COUNT_FOLLOWING,
        topic = MQConstants.TOPIC_COUNT_FOLLOWING)
public class TodayUserFollowIncrementData2DBConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${table.shards}")
    private int shards;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private InsertRecordMapper insertRecordMapper;

    @Override
    public void onMessage(String s) {
        log.info("## TodayUserFollowIncrementData2DBConsumer 消费到了 MQ: {}", s);
        CountFollowUnfollowDTO countFollowUnfollowDTO = JsonUtil.parseObject(s, CountFollowUnfollowDTO.class);
        if (Objects.isNull(countFollowUnfollowDTO)) return;
        Long userId = countFollowUnfollowDTO.getUserId();
        Long targetUserId = countFollowUnfollowDTO.getTargetUserId();
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String userRedisKey = RedisKeyConstants.buildBloomUserFollowListKey(date);
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/bloom_today_user_following_check.lua")));
        redisScript.setResultType(Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(userRedisKey), userId);
        // Lua 脚本：添加到布隆过滤器
        RedisScript<Long> bloomAddScript = RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);
        if (Objects.equals(result, 0L)) {
            //不存在就插入
            long userHashKey = userId % shards;
            String tableNameSuffix = TableConstant.buildTableNameSuffix(date, userHashKey);
            try {
                insertRecordMapper.insert2DataAlignUserFollowingCountTempTable(tableNameSuffix, userId);
            } catch (Exception e) {
                log.error("", e);
            }
            redisTemplate.execute(bloomAddScript, Collections.singletonList(userRedisKey), userId);
        }

        String targetUserRedisKey = RedisKeyConstants.buildBloomUserFansListKey(date);
        Long result1 = redisTemplate.execute(redisScript, Collections.singletonList(targetUserRedisKey), targetUserId);
        if (Objects.equals(result1, 0L)) {
            long targetHashKey = targetUserId % shards;
            String tableNameSuffix = TableConstant.buildTableNameSuffix(date, targetHashKey);
            try {
                insertRecordMapper.insert2DataAlignUserFansCountTempTable(tableNameSuffix, targetUserId);
            } catch (Exception e) {
                log.error("", e);
            }
            redisTemplate.execute(bloomAddScript, Collections.singletonList(targetUserRedisKey), targetUserId);
        }
    }
}
    
