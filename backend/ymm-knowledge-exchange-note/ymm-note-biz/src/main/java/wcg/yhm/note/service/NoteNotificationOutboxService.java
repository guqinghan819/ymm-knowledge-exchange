package wcg.yhm.note.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import wcg.yhm.note.api.domain.event.NoteModerationEvent;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.domain.dataobject.NoteNotificationOutbox;
import wcg.yhm.note.domain.mapper.NoteNotificationOutboxMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NoteNotificationOutboxService {
    @Resource
    private NoteNotificationOutboxMapper mapper;

    public void appendDowned(Note note, String reasonCode, String reason, LocalDateTime occurredAt) {
        LocalDateTime now = LocalDateTime.now();
        mapper.insert(NoteNotificationOutbox.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(NoteModerationEvent.EVENT_NOTE_DOWNED)
                .noteId(note.getId())
                .recipientId(note.getCreatorId())
                .noteTitle(note.getTitle())
                .reasonCode(reasonCode)
                .reason(reason)
                .occurredAt(occurredAt)
                .schemaVersion(NoteModerationEvent.SCHEMA_VERSION_V1)
                .status(0)
                .attempts(0)
                .nextRetryTime(now)
                .createTime(now)
                .updateTime(now)
                .build());
    }
}
