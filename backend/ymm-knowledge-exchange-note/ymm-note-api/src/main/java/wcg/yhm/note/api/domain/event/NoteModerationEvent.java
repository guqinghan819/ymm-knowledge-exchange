package wcg.yhm.note.api.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记平台治理事件契约。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteModerationEvent {
    public static final String TOPIC = "NoteModerationTopic";
    public static final String TAG_DOWNED = "Downed";
    public static final String EVENT_NOTE_DOWNED = "NOTE_DOWNED";
    public static final int SCHEMA_VERSION_V1 = 1;

    private String eventId;
    private String eventType;
    private Long noteId;
    private Long recipientId;
    private String noteTitle;
    private String reasonCode;
    private String reason;
    private LocalDateTime occurredAt;
    private Integer schemaVersion;
}
