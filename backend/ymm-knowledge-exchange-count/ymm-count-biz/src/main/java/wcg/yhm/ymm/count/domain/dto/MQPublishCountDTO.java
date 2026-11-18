package wcg.yhm.ymm.count.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-15
 * @Description:
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MQPublishCountDTO {
    private Long userId;
    /**
     * 1 发布笔记 0 删除笔记
     */
    private Integer type;

    private LocalDateTime createTime;
}
    
