package wcg.yhm.ymm.count.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-10
 * @Description: 点赞、取消点赞
 * @Version: 1.0
 */

@AllArgsConstructor
@Data
@NoArgsConstructor
public class CountLikeUnlikeDTO {
    /**
     * 1: 点赞
     * 0: 取消点赞
     */
    private Integer type;
    private Long noteId;
    private Long userId;
    private LocalDateTime createTime;
    private Long creatorId;
}
    
