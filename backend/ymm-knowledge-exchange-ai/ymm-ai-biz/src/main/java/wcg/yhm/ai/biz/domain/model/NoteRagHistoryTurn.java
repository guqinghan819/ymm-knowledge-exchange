package wcg.yhm.ai.biz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 最近一轮笔记 RAG 会话，仅用于消解当前问题中的指代。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagHistoryTurn {
    private String question;
    private String answer;
}
