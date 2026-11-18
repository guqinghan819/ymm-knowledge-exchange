package wcg.yhm.ai.biz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagChildChunk;

/** Qdrant RRF 候选及精排调试信息。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagCandidate {
    private String pointId;
    private Long noteId;
    private String parentId;
    private Double rrfScore;
    private Integer rrfRank;
    private Double rerankScore;
    private Integer rerankRank;
    private String recallSource;
    private String title;
    private String topicName;
    private String creatorName;
    private String cover;
    private AiNoteRagChildChunk child;

    public String documentText() {
        return "标题：" + (title == null ? "" : title) + "\n话题："
                + (topicName == null ? "" : topicName) + "\n正文："
                + (child == null || child.getChildText() == null ? "" : child.getChildText());
    }
}
