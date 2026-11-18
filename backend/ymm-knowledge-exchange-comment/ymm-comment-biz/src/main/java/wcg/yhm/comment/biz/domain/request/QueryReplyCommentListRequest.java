package wcg.yhm.comment.biz.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 二级评论列表查询请求
 */
@Data
public class QueryReplyCommentListRequest {

    @NotNull(message = "笔记id不能为空")
    private Long noteId;

    @NotNull(message = "一级评论id不能为空")
    private Long parentId;

    private Long cursor;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 50, message = "每页数量不能大于50")
    private Integer limit = 20;
}
