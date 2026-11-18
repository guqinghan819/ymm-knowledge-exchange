package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-10
 * @Description: 取消点赞请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoteUnlikeRequest {
    @NotNull(message = "笔记id不能为空")
    private Long noteId;
}
    
