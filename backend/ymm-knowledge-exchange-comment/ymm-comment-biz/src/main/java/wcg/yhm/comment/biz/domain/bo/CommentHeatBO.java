package wcg.yhm.comment.biz.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: 评论业务类
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentHeatBO {

    /**
     * 评论 ID
     */
    private Long id;

    /**
     * 热度值
     */
    private Double heat;
}
    
