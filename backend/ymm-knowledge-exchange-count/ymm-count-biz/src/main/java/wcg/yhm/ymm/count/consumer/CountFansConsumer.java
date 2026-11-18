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
import wcg.yhm.ymm.count.domain.dto.CountFollowUnfollowDTO;
import wcg.yhm.ymm.count.enums.CountTypeEnum;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-04
 * @Description: 粉丝计数消费者
 * @Version: 1.0
 */

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "ymm_group" + MQConstants.TOPIC_COUNT_FANS,
        topic = MQConstants.TOPIC_COUNT_FANS)
public class CountFansConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    private final BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) //队列最大缓存大小
            .batchSize(1000) //满1000聚合一次
            .linger(Duration.ofSeconds(1)) //不管batchSize有没有满，到1秒就聚合
            .setConsumerEx(this::consumerMessage) //执行方法
            .build();
    @Override
    public void onMessage(String body) {
        bufferTrigger.enqueue(body);
    }

    private void consumerMessage (List<String> bodys) {
        log.info("==> 聚合消息, size: {}", bodys.size());
        log.info("==> 聚合消息, {}", JsonUtil.toJsonString(bodys));

        List<CountFollowUnfollowDTO> countFollowUnfollowDTOS = bodys.stream()
                .map(body -> JsonUtil.parseObject(body, CountFollowUnfollowDTO.class))
                .toList();

        //按照目标用户进行分组
        Map<Long, List<CountFollowUnfollowDTO>> groupMap = countFollowUnfollowDTOS.stream()
                .collect(Collectors.groupingBy(CountFollowUnfollowDTO::getTargetUserId));

        Map<Long, Integer> countMap = new HashMap<>();
        //按照分组统计数据
        for (Map.Entry<Long, List<CountFollowUnfollowDTO>> entry : groupMap.entrySet()) {
            List<CountFollowUnfollowDTO> values = entry.getValue();
            int finalCount = 0;
            for (CountFollowUnfollowDTO dto : values) {
                CountTypeEnum typeEnum = CountTypeEnum.getEnum(dto.getType());
                if (Objects.isNull(typeEnum)) continue;
                switch (typeEnum) {
                    case FOLLOW -> finalCount += 1;
                    case UNFOLLOW -> finalCount -= 1;
                }
            }
            countMap.put(entry.getKey(), finalCount);
        }
        log.info("=======> countMap: {}", countMap);

        //redis更新
        countMap.forEach((k, v) -> {
            String redisKey = RedisKeyConstants.COUNT_USER_KEY_PREFIX + k;
            //查看hash是否存在
            Boolean isExists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(isExists)) {
                //存在才更新
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FANS_TOTAL, v);
            }
        });
        //发送mq进行db落库
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(countMap)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("========> 粉丝计数落库消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("=======> 粉丝计数落库消息发送失败", throwable);
            }
        });

    }
}
    
