package wcg.yhm.ymm.count.domain.dto;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-14
 * @Description: 聚合计数点赞
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregationCountLikeDTO {
    //创作者Id
    private Long creatorId;
    //笔记id
    private Long noteId;
    //点赞计数
    private Long count;
}
    
