package wcg.yhm.comment.biz.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论发布响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishCommentResponse {

    private Long commentId;

    private LocalDateTime createTime;
}
