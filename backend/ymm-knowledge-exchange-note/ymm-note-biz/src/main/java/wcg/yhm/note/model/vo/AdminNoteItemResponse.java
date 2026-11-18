package wcg.yhm.note.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @Description: 管理端笔记列表项响应
 */
@Data
public class AdminNoteItemResponse {

    private Long id;

    private String title;

    /**
     * 封面(图文取第一张图，视频取视频地址)
     */
    private String cover;

    private Integer type;

    private Integer status;

    private Integer visible;

    private Boolean isTop;

    private Long creatorId;

    private String creatorName;

    private Long topicId;

    private String topicName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
    private String reviewMethod;
    private String reviewReason;
    private LocalDateTime reviewedAt;
    private Integer reviewVersion;
    private String reviewTaskStatus;
    private String reviewInternalError;

    private Long likeCount = 0L;

    private Long collectCount = 0L;

    private Long commentCount = 0L;
}
