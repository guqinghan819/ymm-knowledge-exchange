package wcg.yhm.note.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.note.constants.MQConstant;
import wcg.yhm.note.utils.CaffeineUtil;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-24
 * @Description: 删除笔记本地缓存消费者
 * @Version: 1.0
 */

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "ymm_group" + MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, //用组区分消费者
topic = MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, //消费主题
messageModel = MessageModel.BROADCASTING) //广播模式
public class DeleteNoteLocalCacheConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String s) {
        Long noteId = Long.valueOf(s);
        log.info("## 消费者消费成功， noteId: {}", noteId);
        CaffeineUtil.deleteNoteDetail(noteId);
    }
}
    
