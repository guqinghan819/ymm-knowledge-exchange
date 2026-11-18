package wcg.yhm.ymm.count.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-12
 * @Description:
 * @Version: 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CountCollectUnCollectDTO {

    private Long userId;
    private Long noteId;
    //1: 收藏 0: 取消收藏
    private Integer status;
    private LocalDateTime createTime;
    private Long creatorId;
}
    
