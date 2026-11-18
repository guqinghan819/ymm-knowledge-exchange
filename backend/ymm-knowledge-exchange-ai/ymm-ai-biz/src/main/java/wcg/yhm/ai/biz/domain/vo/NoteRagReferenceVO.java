package wcg.yhm.ai.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 笔记 RAG 引用展示对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagReferenceVO {

    private Integer citationNo;

    private Long noteId;

    private String parentId;

    private String title;

    private String topicName;

    private String creatorName;

    private String cover;

    private String snippet;

    /** @deprecated 仅调试兼容，不代表可跨请求比较的置信度。 */
    @Deprecated
    private Double score;

    /** @deprecated 仅调试兼容。 */
    @Deprecated
    private String matchedQuery;
}
