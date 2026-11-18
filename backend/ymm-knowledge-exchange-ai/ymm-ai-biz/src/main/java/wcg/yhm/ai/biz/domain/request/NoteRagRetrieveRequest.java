package wcg.yhm.ai.biz.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记 RAG 检索调试请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagRetrieveRequest {

    @NotBlank(message = "问题不能为空")
    private String query;
}
