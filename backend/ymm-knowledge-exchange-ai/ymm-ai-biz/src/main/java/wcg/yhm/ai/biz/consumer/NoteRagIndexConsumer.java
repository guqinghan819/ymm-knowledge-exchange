package wcg.yhm.ai.biz.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.ai.biz.service.INoteRagChunkService;
import wcg.yhm.note.api.domain.event.NoteRagIndexMessage;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.Objects;

/** 消费完整生命周期事件；事件只负责触发，消费者始终重新读取笔记当前状态。 */
@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_" + NoteRagIndexMessage.TOPIC_NOTE_RAG_INDEX,
        topic = NoteRagIndexMessage.TOPIC_NOTE_RAG_INDEX)
public class NoteRagIndexConsumer implements RocketMQListener<String> {

    @Resource
    private INoteRagChunkService noteRagChunkService;

    @Override
    public void onMessage(String messageBody) {
        log.info("## NoteRagIndexConsumer消费了 message: {}", messageBody);
        NoteRagIndexMessage message = JsonUtil.parseObject(messageBody, NoteRagIndexMessage.class);
        if (Objects.isNull(message) || Objects.isNull(message.getNoteId())) {
            log.warn("RAG分片消息为空或noteId为空: {}", messageBody);
            return;
        }
        if (!Objects.equals(NoteRagIndexMessage.EVENT_NOTE_APPROVED, message.getEventType())
                && !Objects.equals(NoteRagIndexMessage.EVENT_UPSERT, message.getEventType())
                && !Objects.equals(NoteRagIndexMessage.EVENT_DELETE, message.getEventType())) {
            log.info("RAG分片消息事件类型不匹配，跳过: eventType={}", message.getEventType());
            return;
        }
        try {
            noteRagChunkService.indexApprovedNote(message.getNoteId());
        } catch (RuntimeException e) {
            log.error("RAG分片消费失败: noteId={}", message.getNoteId(), e);
            throw e;
        }
    }
}
