package wcg.yhm.ai.biz.service;

import wcg.yhm.ai.biz.domain.model.NoteRagHistoryTurn;
import wcg.yhm.ai.biz.domain.model.NoteRagQueryPlan;

import java.util.List;

/**
 * 笔记 RAG 查询改写服务。
 */
public interface INoteRagQueryRewriteService {

    /**
     * 将用户原始问题改写成多个检索问法。
     */
    NoteRagQueryPlan plan(String query, List<NoteRagHistoryTurn> history);

    default List<String> rewrite(String query) {
        NoteRagQueryPlan plan = plan(query, List.of());
        return plan == null || plan.getExpandedQueries() == null ? List.of() : plan.getExpandedQueries();
    }
}
