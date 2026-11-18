package wcg.yhm.ymm.data.align.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-12
 * @Description:
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MQCollectUnCollectDTO {

    private Long userId;

    private Long noteId;
    // 1: 收藏 0 : 取消收藏
    private Integer status;
    private LocalDateTime createTime;
    private Long creatorId; //创作者id
}
    
