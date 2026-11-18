package wcg.yhm.ai.biz.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记 RAG 问答请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagChatRequest {

    @NotBlank(message = "问题不能为空")
    private String query;

    @NotBlank(message = "会话id不能为空")
    private String conversationId;
}
