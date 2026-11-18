package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-24
 * @Description: 笔记置顶请求
 * @Version: 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SetTopRequest {

    @NotNull(message = "笔记id不能为空")
    private Long noteId;

    /**
     * true为置顶， false为不置顶
     */
    @NotNull(message = "置顶状态不能为空")
    private Boolean isTop;
}
    
