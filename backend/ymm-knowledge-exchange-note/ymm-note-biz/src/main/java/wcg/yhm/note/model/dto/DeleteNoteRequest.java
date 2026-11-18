package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-24
 * @Description: 删除笔记请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteNoteRequest implements Serializable {
    @NotNull(message = "笔记id不能为空")
    private Long noteId;
}
    
