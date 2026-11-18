package wcg.yhm.comment.biz.domain.request;

import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-03
 * @Description: 发布评论请求
 * @Version: 1.0
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishCommentRequest {

    @NotNull(message = "笔记id不能为空")
    private Long noteId; //笔记id
    private String imgUrl;
    private String content; //评论内容
    private Long replyCommentId; //回复的哪个评论（评论 ID）
}
    
