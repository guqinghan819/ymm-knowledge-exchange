package wcg.yhm.note.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteAnalysisNoteItemResponse {

    private Long noteId;

    private String title;

    private String cover;

    private Integer type;

    private LocalDateTime createTime;

    private Long likeCount = 0L;

    private Long collectCount = 0L;

    private Long commentCount = 0L;

    private String channelName;
}
