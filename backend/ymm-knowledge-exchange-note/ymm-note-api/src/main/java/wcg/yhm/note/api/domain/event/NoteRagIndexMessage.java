package wcg.yhm.note.api.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 笔记 RAG 索引事件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagIndexMessage implements Serializable {

    public static final String TOPIC_NOTE_RAG_INDEX = "NoteRagIndexTopic";
    public static final String TAG_NOTE_APPROVED = "Approved";
    public static final String EVENT_NOTE_APPROVED = "NOTE_APPROVED";
    public static final String TAG_SYNC = "Sync";
    public static final String EVENT_UPSERT = "UPSERT";
    public static final String EVENT_DELETE = "DELETE";
    public static final int SCHEMA_VERSION_V2 = 2;

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long noteId;

    private String eventType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime noteUpdatedAt;

    private Integer schemaVersion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime triggerTime;
}
