package wcg.yhm.note.api.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询 RAG 索引用笔记详情请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryNoteRagIndexDetailRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "笔记id不能为空")
    private Long noteId;
}
