package wcg.yhm.note.consumer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import wcg.yhm.note.api.domain.event.MQCollectUnCollectDTO;
import wcg.yhm.note.constants.MQConstant;
import wcg.yhm.note.domain.dataobject.NoteCollection;
import wcg.yhm.note.domain.mapper.NoteCollectionMapper;
import wcg.yhm.note.enums.NoteCollectStatus;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-12
 * @Description: 收藏、取消收藏消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstant.TOPIC_NOTE_COLLECT_OR_UN_COLLECT,
topic = MQConstant.TOPIC_NOTE_COLLECT_OR_UN_COLLECT,
consumeMode = ConsumeMode.ORDERLY)
@Slf4j
public class CollectUnCollectConsumer implements RocketMQListener<Message> {
    @Resource
    private NoteCollectionMapper noteCollectionMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Override
    public void onMessage(Message message) {
        rateLimiter.acquire();
        String tags = message.getTags();
        String jsonStr = new String(message.getBody());
        log.info("===========> 开始处理收藏、取消收藏落库消息, data:{}", jsonStr);
        if (tags.equals(MQConstant.TAG_COLLECT)) {
            handleCollect(jsonStr);
        } else if (tags.equals(MQConstant.TAG_UN_COLLECT)) {
            handleUnCollect(jsonStr);
        }
    }

    /**
     * 处理取消收藏落库
     * @param jsonStr
     */
    private void handleUnCollect(String jsonStr) {
        MQCollectUnCollectDTO mqCollectUnCollectDTO = JsonUtil.parseObject(jsonStr, MQCollectUnCollectDTO.class);
        if (mqCollectUnCollectDTO == null) return;
        int affect = noteCollectionMapper.update(new UpdateWrapper<NoteCollection>()
                .eq("user_id", mqCollectUnCollectDTO.getUserId())
                .eq("note_id", mqCollectUnCollectDTO.getNoteId())
                .eq("status", NoteCollectStatus.COLLECT.getCode())
                .set("status", NoteCollectStatus.NOT_COLLECT.getCode())
                .set("create_time", mqCollectUnCollectDTO.getCreateTime()));
        if (affect > 0) {
            log.info("=========> 取消收藏落库成功");
            //发送计数消息
            org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(jsonStr).build();
            rocketMQTemplate.asyncSend(MQConstant.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("========> 发送取消笔记收藏计数消息成功, result: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("## 发送取消笔记收藏计数消息失败, error: ", throwable);
                }
            });
        } else {
            log.error("## 取消收藏落库失败");
        }
    }

    /**
     * 处理收藏落库
     * @param jsonStr
     */
    private void handleCollect(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) return;
        MQCollectUnCollectDTO mqCollectUnCollectDTO = JsonUtil.parseObject(jsonStr, MQCollectUnCollectDTO.class);
        NoteCollection noteCollection = BeanUtil.copyProperties(mqCollectUnCollectDTO, NoteCollection.class);
        boolean result = noteCollectionMapper.saveOrUpdateCollect(noteCollection);
        if (result) {
            log.info("=========> 收藏落库成功");
            //发送计数消息
            org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(jsonStr).build();
            rocketMQTemplate.asyncSend(MQConstant.TOPIC_COUNT_NOTE_COLLECT, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("========> 发送笔记收藏计数消息成功, result: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("## 发送笔记收藏计数消息失败, error: ", throwable);
                }
            });
        } else {
            log.error("## 收藏落库失败");
        }


    }



}
    
