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
import wcg.yhm.ymm.count.domain.dto.AggregationCountLikeDTO;
import wcg.yhm.ymm.count.domain.dto.CountLikeUnlikeDTO;
import wcg.yhm.ymm.count.enums.NoteLikeEnum;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-11
 * @Description: 点赞、取消点赞计数消费者
 * @Version: 1.0
 */

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_NOTE_LIKE_OR_UNLIKE,
topic = MQConstants.TOPIC_NOTE_LIKE_OR_UNLIKE)
public class CountLikeUnlikeConsumer implements RocketMQListener<String> {

    private final BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::messageConsumer)
            .build();
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(String s) {
        bufferTrigger.enqueue(s);
    }

    private void messageConsumer(List<String> bodys) {
        log.info("==> 聚合消息, size: {}", bodys.size());
        log.info("==> 聚合消息, {}", JsonUtil.toJsonString(bodys));
        List<CountLikeUnlikeDTO> list =
                bodys.stream().map(s -> JsonUtil.parseObject(s, CountLikeUnlikeDTO.class)).toList();
        //按照笔记id进行分组
        Map<Long, List<CountLikeUnlikeDTO>> groupMap =
                list.stream().collect(Collectors.groupingBy(CountLikeUnlikeDTO::getNoteId));


        //操作统计
        List<AggregationCountLikeDTO> countList = new ArrayList<>();
        for (Map.Entry<Long, List<CountLikeUnlikeDTO>> entry : groupMap.entrySet()) {
            List<CountLikeUnlikeDTO> value = entry.getValue();
            long count = 0;
            Long creatorId = null;
            Long noteId = entry.getKey();
            for (CountLikeUnlikeDTO countLikeUnlikeDTO : value) {
                NoteLikeEnum noteLikeEnum = NoteLikeEnum.getEnum(countLikeUnlikeDTO.getType());
                if (noteLikeEnum == null) continue;
                creatorId = countLikeUnlikeDTO.getCreatorId();
                switch (noteLikeEnum) {
                    case LIKE -> count += 1;
                    case UNLIKE -> count -= 1;
                }
            }
            AggregationCountLikeDTO aggregationCountLikeDTO = new AggregationCountLikeDTO();
            aggregationCountLikeDTO.setNoteId(noteId);
            aggregationCountLikeDTO.setCount(count);
            aggregationCountLikeDTO.setCreatorId(creatorId);
            countList.add(aggregationCountLikeDTO);
        }

        log.info("========> 聚合后的数据, data: {}", JsonUtil.toJsonString(countList));
        //redis 更新
        countList.forEach(a -> {
            Long count = a.getCount();
            Long noteId = a.getNoteId();
            Long creatorId = a.getCreatorId();
            //笔记维度计数
            String redisKey = RedisKeyConstants.COUNT_NOTE_KEY_PREFIX + noteId;
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                //存在就更新
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }
            //用户维度计数
            redisKey = RedisKeyConstants.COUNT_USER_KEY_PREFIX + creatorId;
            exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                //存在就更新
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }
        });

        //发送落库消息
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(countList)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("========>笔记获赞计数落库消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("========>笔记获赞计数落库消息发送失败, error:", throwable);
            }
        });
    }
}
    
