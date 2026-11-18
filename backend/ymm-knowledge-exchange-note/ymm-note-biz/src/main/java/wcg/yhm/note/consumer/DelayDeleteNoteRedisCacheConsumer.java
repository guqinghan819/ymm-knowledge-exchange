package wcg.yhm.note.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import wcg.yhm.note.constants.MQConstant;
import wcg.yhm.note.constants.RedisKeyConstant;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-24
 * @Description: 延迟删除redis笔记缓存消费者
 * @Version: 1.0
 */


@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstant.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE,
topic = MQConstant.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE)
public class DelayDeleteNoteRedisCacheConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public void onMessage(String s) {
        String redisKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + s;
        redisTemplate.delete(redisKey);
        log.info("## 延迟删除redis note cache 消费成功， noteId: {}", s);
    }
}
    
