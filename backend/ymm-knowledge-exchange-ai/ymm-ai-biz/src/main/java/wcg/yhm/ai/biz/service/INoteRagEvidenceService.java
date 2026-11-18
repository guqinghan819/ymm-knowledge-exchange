package wcg.yhm.ai.biz.service;

import wcg.yhm.ai.biz.domain.model.NoteRagCandidate;
import wcg.yhm.ai.biz.domain.model.NoteRagEvidenceDecision;

import java.util.List;

public interface INoteRagEvidenceService {
    NoteRagEvidenceDecision evaluate(String query, List<NoteRagCandidate> candidates);
}
