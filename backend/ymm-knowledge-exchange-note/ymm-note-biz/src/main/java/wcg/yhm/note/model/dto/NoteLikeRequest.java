package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-09
 * @Description: 笔记点赞请求
 * @Version: 1.0
 */

@AllArgsConstructor
@Data
@NoArgsConstructor
public class NoteLikeRequest {
    @NotNull(message = "笔记id不能为空")
    private Long noteId;
}
    
