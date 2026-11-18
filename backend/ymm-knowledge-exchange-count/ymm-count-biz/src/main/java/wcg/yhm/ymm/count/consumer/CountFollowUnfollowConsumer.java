package wcg.yhm.ymm.count.consumer;

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

import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-04
 * @Description: 关注、取关计数消息消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group" + MQConstants.TOPIC_COUNT_FOLLOWING,
topic = MQConstants.TOPIC_COUNT_FOLLOWING)
@Slf4j
public class CountFollowUnfollowConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void onMessage(String body) {
        CountFollowUnfollowDTO dto = JsonUtil.parseObject(body, CountFollowUnfollowDTO.class);
        if (dto == null) {
           return;
        }
        Long userId = dto.getUserId();
        Integer type = dto.getType();
        String redisKey = RedisKeyConstants.COUNT_USER_KEY_PREFIX + userId;
        Boolean isExisted = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(isExisted)) {
            long count = Objects.equals(type, CountTypeEnum.FOLLOW.getValue()) ? 1 : -1;
            redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, count);
        }

        // 发送 MQ, 关注数写库
        // 构建消息对象
        Message<String> message = MessageBuilder.withPayload(body).build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数入库】MQ 发送异常: ", throwable);
            }
        });

    }
}
    
