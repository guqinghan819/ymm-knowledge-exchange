package wcg.yhm.note.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyNoteItemResponse {

    private Long id;

    private String title;

    private String cover;

    private Integer type;

    private LocalDateTime createTime;

    private Long commentCount = 0L;

    private Long likeCount = 0L;

    private Long collectCount = 0L;

    private Boolean isTop = false;

    private Integer status;
    private String reviewReason;
}
