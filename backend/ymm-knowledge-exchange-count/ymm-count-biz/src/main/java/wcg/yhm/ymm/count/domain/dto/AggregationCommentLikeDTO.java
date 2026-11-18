package wcg.yhm.ymm.count.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合后的评论点赞计数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregationCommentLikeDTO {

    private Long commentId;

    private Long count;
}
