package wcg.yhm.ymm.count.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.constant.RedisKeyConstants;
import wcg.yhm.ymm.count.domain.dto.AggregationCountCollectDTO;
import wcg.yhm.ymm.count.domain.dto.CountCollectUnCollectDTO;
import wcg.yhm.ymm.count.enums.NoteCollectEnum;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-12
 * @Description: 笔记收藏、取消收藏计数消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT,
topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT)
@Slf4j
public class CountCollectConsumer implements RocketMQListener<String> {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    private final BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumerMessage)
            .build();


    @Override
    public void onMessage(String s) {
        bufferTrigger.enqueue(s);
    }

    private void consumerMessage(List<String> bodys) {
        log.info("==> 聚合消息, size: {}", bodys.size());
        log.info("==> 聚合消息, {}", JsonUtil.toJsonString(bodys));
        //jsonStr -> bean
        List<CountCollectUnCollectDTO> list =
                bodys.stream().map(b -> JsonUtil.parseObject(b, CountCollectUnCollectDTO.class)).toList();

        //按照笔记id进行分组
        Map<Long, List<CountCollectUnCollectDTO>> groupMap =
                list.stream().collect(Collectors.groupingBy(CountCollectUnCollectDTO::getNoteId));
        //按照操作进行统计
        List<AggregationCountCollectDTO> countList = new ArrayList<>();
        for (Map.Entry<Long, List<CountCollectUnCollectDTO>> entry : groupMap.entrySet()) {
            long count = 0;
            Long noteId = entry.getKey();
            Long creatorId = null;
            for (CountCollectUnCollectDTO dto : entry.getValue()) {
                NoteCollectEnum collectEnum = NoteCollectEnum.getByCode(dto.getStatus());
                if (Objects.isNull(collectEnum)) continue;
                creatorId = dto.getCreatorId();
                switch (collectEnum) {
                    case COLLECT -> count += 1;
                    case NOT_COLLECT -> count -= 1;
                }
            }
            AggregationCountCollectDTO aggregationCountCollectDTO = new AggregationCountCollectDTO(creatorId, noteId, count );
            countList.add(aggregationCountCollectDTO);
        }
        //更新redis
        countList.forEach(c -> {
            //笔记维度收藏
            Long noteId = c.getNoteId();
            String redisKey = RedisKeyConstants.COUNT_NOTE_KEY_PREFIX + noteId;
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_COLLECTION_TOTAL, c.getCount());
            }
            //用户维度收场
            Long creatorId = c.getCreatorId();
            redisKey = RedisKeyConstants.COUNT_USER_KEY_PREFIX + creatorId;
            exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_COLLECTION_TOTAL, c.getCount());
            }
        });

        //发送计数落库消息
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(countList)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=======> 笔记收藏计数落库消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("## 笔记收藏计数落库消息发送失败, error: ", throwable);
            }
        });
    }
}
    
