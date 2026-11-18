package wcg.yhm.ai.biz.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wcg.yhm.ai.biz.domain.vo.NoteRagReferenceVO;
import wcg.yhm.ai.biz.domain.model.NoteRagCandidate;

import java.util.List;

/**
 * 笔记 RAG 检索调试响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagRetrieveResponse {

    private String query;

    private List<String> queries;

    private String standaloneQuery;

    private List<String> exactTerms;

    private Boolean answerable;

    private String evidenceReason;

    private List<NoteRagCandidate> candidates;

    private List<NoteRagReferenceVO> references;
}
