package wcg.yhm.text.api.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description: 批量增加评论内容请求
 * @Version: 1.0
 */


@AllArgsConstructor
@NoArgsConstructor
@Data
public class BatchAddCommentRequest {

    @NotEmpty(message = "评论内容集合不能为空")
    @Valid  // 指定集合内的评论 DTO，也需要进行参数校验
    private List<CommentContentDTO> contentDTOList;
}
    
