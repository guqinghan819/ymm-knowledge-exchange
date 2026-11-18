package wcg.yhm.ai.biz.service;

import wcg.yhm.ai.biz.domain.model.NoteRagCandidate;

import java.util.List;

public interface INoteRagRerankService {
    List<NoteRagCandidate> rerank(String query, List<NoteRagCandidate> candidates);
}
