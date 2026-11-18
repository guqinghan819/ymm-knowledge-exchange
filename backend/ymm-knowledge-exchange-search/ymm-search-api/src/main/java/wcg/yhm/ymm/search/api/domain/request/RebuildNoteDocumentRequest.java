package wcg.yhm.ymm.search.api.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description: 重构笔记文档请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RebuildNoteDocumentRequest {
    @NotNull(message = "笔记id不能为空")
    private Long noteId;
}
    
