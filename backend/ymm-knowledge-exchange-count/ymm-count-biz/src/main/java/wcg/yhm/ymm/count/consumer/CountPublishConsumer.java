package wcg.yhm.ymm.count.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.constant.RedisKeyConstants;
import wcg.yhm.ymm.count.domain.dto.MQPublishCountDTO;
import wcg.yhm.ymm.count.mapper.UserCountMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-15
 * @Description: 发布笔记计数消费者
 * @Version: 1.0
 */

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_COUNT_NOTE_PUBLISH,
topic = MQConstants.TOPIC_COUNT_NOTE_PUBLISH)
public class CountPublishConsumer implements RocketMQListener<Message> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserCountMapper userCountMapper;
    @Override
    public void onMessage(Message message) {
        String tags = message.getTags();
        String jsonStr = new String(message.getBody());
        if (tags.equals(MQConstants.TAG_PUBLISH)) {
            handlePublish(jsonStr);
        } else if (tags.equals(MQConstants.TAG_DELETE)) {
            handleDelete(jsonStr);
        }
    }

    /**
     * 处理删除笔记计数
     * @param jsonStr
     */
    private void handleDelete(String jsonStr) {
        MQPublishCountDTO mqPublishCountDTO = JsonUtil.parseObject(jsonStr, MQPublishCountDTO.class);
        if (mqPublishCountDTO == null) return;
        Long userId = mqPublishCountDTO.getUserId();
        updateUserNoteTotal(userId, -1);
        log.info("======》 发布笔记计数落库成功");

    }

    /**
     * 处理发布笔记计数
     * @param jsonStr
     */
    private void handlePublish(String jsonStr) {
        MQPublishCountDTO mqPublishCountDTO = JsonUtil.parseObject(jsonStr, MQPublishCountDTO.class);
        if (mqPublishCountDTO == null) return;
        Long userId = mqPublishCountDTO.getUserId();
        updateUserNoteTotal(userId, 1);
        log.info("======》 发布笔记计数落库成功");
    }

    private void updateUserNoteTotal(Long userId, long count) {
        if (userId == null) {
            return;
        }
        //更新redis
        String redisKey = RedisKeyConstants.COUNT_USER_KEY_PREFIX + userId;
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            Long total = redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_PUBLISH_TOTAL, count);
            if (total != null && total < 0) {
                redisTemplate.opsForHash().put(redisKey, RedisKeyConstants.FIELD_PUBLISH_TOTAL, 0L);
            }
        }
        userCountMapper.saveOrUpdatePublishTotal(userId, count);
    }
}
    
