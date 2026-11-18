package wcg.yhm.ymm.count.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论点赞/取消点赞计数消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CountCommentLikeUnlikeDTO {

    /**
     * 1: 点赞, 0: 取消点赞
     */
    private Integer type;

    private Long commentId;

    private Long userId;

    private LocalDateTime createTime;
}
