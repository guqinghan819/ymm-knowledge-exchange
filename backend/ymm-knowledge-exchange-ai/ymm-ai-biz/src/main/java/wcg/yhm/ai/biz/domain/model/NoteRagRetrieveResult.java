package wcg.yhm.ai.biz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wcg.yhm.ai.biz.domain.vo.NoteRagReferenceVO;

import java.util.List;

/**
 * 笔记 RAG 内部检索结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagRetrieveResult {

    private String query;

    private List<String> queries;

    private String standaloneQuery;

    private List<String> exactTerms;

    private boolean answerable;

    private String evidenceReason;

    private List<NoteRagCandidate> candidates;

    private List<NoteRagContext> contexts;

    /** 可展示的召回来源；证据不足时也会保留候选笔记。 */
    private List<NoteRagReferenceVO> references;
}
