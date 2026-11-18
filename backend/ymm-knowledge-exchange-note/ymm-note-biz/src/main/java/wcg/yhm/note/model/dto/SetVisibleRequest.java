package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-24
 * @Description: 设置是否可见请求
 * @Version: 1.0
 */

@AllArgsConstructor
@Data
@NoArgsConstructor
public class SetVisibleRequest {

    @NotNull(message = "笔记id不能为空")
    private Long noteId;
}
    
