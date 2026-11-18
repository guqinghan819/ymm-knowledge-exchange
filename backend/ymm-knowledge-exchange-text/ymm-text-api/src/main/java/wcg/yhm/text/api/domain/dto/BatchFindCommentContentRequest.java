package wcg.yhm.text.api.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: 批量查询评论内容请求
 * @Version: 1.0
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchFindCommentContentRequest {

    /**
     * 笔记 ID
     */
    @NotNull(message = "评论 ID 不能为空")
    private Long noteId;

    @NotEmpty(message = "评论内容 Key 集合")
    @Valid  // 指定集合中的 DTO 也需要进行参数校验
    private List<FindCommentContentDTO> commentContentKeys;
}
    
