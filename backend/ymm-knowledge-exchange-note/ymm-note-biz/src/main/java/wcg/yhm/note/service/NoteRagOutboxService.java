package wcg.yhm.note.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import wcg.yhm.note.api.domain.event.NoteRagIndexMessage;
import wcg.yhm.note.domain.dataobject.NoteRagOutbox;
import wcg.yhm.note.domain.mapper.NoteRagOutboxMapper;

import java.time.LocalDateTime;
import java.util.UUID;

/** 在笔记业务事务内追加 RAG 同步事件。 */
@Service
public class NoteRagOutboxService {

    @Resource
    private NoteRagOutboxMapper mapper;

    public void append(Long noteId, String eventType, LocalDateTime noteUpdatedAt) {
        LocalDateTime now = LocalDateTime.now();
        mapper.insert(NoteRagOutbox.builder()
                .eventId(UUID.randomUUID().toString())
                .noteId(noteId)
                .eventType(eventType)
                .noteUpdatedAt(noteUpdatedAt)
                .schemaVersion(NoteRagIndexMessage.SCHEMA_VERSION_V2)
                .status(0).attempts(0).nextRetryTime(now)
                .createTime(now).updateTime(now).build());
    }
}
