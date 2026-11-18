package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-11
 * @Description: 笔记收藏请求
 * @Version: 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NoteCollectRequest {
    @NotNull(message = "笔记id不能为空")
    private Long noteId;
}
    
