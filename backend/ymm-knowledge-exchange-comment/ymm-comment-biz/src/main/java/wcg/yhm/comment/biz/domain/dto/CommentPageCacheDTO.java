package wcg.yhm.comment.biz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wcg.yhm.comment.biz.domain.response.CommentItemResponse;

import java.util.Collections;
import java.util.List;

/**
 * 评论分页缓存对象，不包含当前用户点赞态。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentPageCacheDTO {

    private List<CommentItemResponse> data;

    private Long nextCursor;

    private boolean hasNext;

    private int size;

    public static CommentPageCacheDTO empty() {
        return CommentPageCacheDTO.builder()
                .data(Collections.emptyList())
                .nextCursor(null)
                .hasNext(false)
                .size(0)
                .build();
    }
}
