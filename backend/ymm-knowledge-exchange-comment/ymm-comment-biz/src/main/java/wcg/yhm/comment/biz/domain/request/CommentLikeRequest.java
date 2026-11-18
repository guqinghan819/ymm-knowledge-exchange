package wcg.yhm.comment.biz.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论点赞请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentLikeRequest {

    @NotNull(message = "评论id不能为空")
    private Long commentId;
}
