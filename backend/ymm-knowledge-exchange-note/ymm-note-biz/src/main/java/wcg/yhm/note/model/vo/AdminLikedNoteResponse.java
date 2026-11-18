package wcg.yhm.note.model.vo;

import lombok.Data;

/**
 * 管理端高赞作品排名项。
 */
@Data
public class AdminLikedNoteResponse {

    private Long noteId;

    private String title;

    private String cover;

    private Integer type;

    private Long creatorId;

    private String creatorName;

    private String creatorAvatar;

    private Long likeCount = 0L;
}
