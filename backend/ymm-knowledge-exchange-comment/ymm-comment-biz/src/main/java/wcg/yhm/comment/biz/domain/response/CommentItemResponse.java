package wcg.yhm.comment.biz.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论列表项
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentItemResponse {

    private Long id;

    private Long noteId;

    private Long userId;

    private String nickname;

    private String avatar;

    private String content;

    private String imageUrl;

    private Integer level;

    private Long parentId;

    private Long replyCommentId;

    private Long replyUserId;

    private String replyNickname;

    private Long childCommentTotal;

    private Long likeTotal;

    private Boolean isLike;

    private LocalDateTime createTime;
}
