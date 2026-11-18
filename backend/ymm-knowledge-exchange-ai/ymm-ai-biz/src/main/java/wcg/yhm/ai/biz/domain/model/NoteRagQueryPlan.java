package wcg.yhm.ai.biz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 一次结构化模型调用生成的检索计划。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagQueryPlan {
    private String standaloneQuery;
    private List<String> exactTerms;
    private List<String> expandedQueries;
}
