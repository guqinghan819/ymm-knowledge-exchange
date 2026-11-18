package wcg.yhm.ymm.count.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.dto.InsertNoteCountRecordDTO;
import wcg.yhm.ymm.count.domain.po.NoteCount;
import wcg.yhm.ymm.count.service.INoteCountService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 初始化计数表笔记记录消费者
 * @Version: 1.0
 */


@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_INSERT_NOTE_COUNT_RECORD,
topic = MQConstants.TOPIC_INSERT_NOTE_COUNT_RECORD)
@Slf4j
@Component
public class InitNoteCountRecord2DBConsumer implements RocketMQListener<String> {
    @Resource
    private INoteCountService noteCountService;
    @Override
    public void onMessage(String s) {
        log.info("## InitNoteCountRecord2DBConsumer消费了 message: {}", s);
        InsertNoteCountRecordDTO insertNoteCountRecordDTO = JsonUtil.parseObject(s, InsertNoteCountRecordDTO.class);
        if (Objects.isNull(insertNoteCountRecordDTO)) {
            return;
        }
        try {
            noteCountService.save(new NoteCount().setNoteId(insertNoteCountRecordDTO.getNoteId()));
            log.info("## 初始化笔记计数记录落库成功, noteId: {}", insertNoteCountRecordDTO.getNoteId());
        } catch (Exception e) {
            log.error("## 初始化笔记记录记录落库失败, e: ", e);
        }

    }
}
    
