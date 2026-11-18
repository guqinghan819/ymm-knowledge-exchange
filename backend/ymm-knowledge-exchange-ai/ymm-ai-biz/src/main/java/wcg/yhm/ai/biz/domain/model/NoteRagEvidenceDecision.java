package wcg.yhm.ai.biz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 候选文本是否足以回答当前问题的严格判定。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagEvidenceDecision {
    private boolean answerable;
    private List<Integer> evidenceIndexes;
    private String reason;

    public static NoteRagEvidenceDecision reject(String reason) {
        return NoteRagEvidenceDecision.builder()
                .answerable(false)
                .evidenceIndexes(List.of())
                .reason(reason)
                .build();
    }
}
