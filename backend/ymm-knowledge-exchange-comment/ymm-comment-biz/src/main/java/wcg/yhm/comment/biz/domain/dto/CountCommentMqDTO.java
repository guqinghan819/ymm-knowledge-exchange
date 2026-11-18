package wcg.yhm.comment.biz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: 评论计数
 * @Version: 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CountCommentMqDTO {

    private Long noteId;

    private Long commentId;

    /**
     * 评论级别
     */
    private Integer level;

    /**
     * 父 ID
     */
    private Long parentId;
}
    
