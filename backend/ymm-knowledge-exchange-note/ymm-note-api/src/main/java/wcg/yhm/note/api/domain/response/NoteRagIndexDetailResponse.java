package wcg.yhm.note.api.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 索引用笔记详情。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagIndexDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long noteId;

    private String title;

    private String content;

    private String contentUuid;

    private Long topicId;

    private String topicName;

    private Long creatorId;

    private String creatorName;

    private String creatorAvatar;

    private String cover;

    private Integer type;

    private Integer visible;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
