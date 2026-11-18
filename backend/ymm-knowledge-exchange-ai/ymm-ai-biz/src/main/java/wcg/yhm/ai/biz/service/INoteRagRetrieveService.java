package wcg.yhm.ai.biz.service;

import wcg.yhm.ai.biz.domain.model.NoteRagRetrieveResult;
import wcg.yhm.ai.biz.domain.model.NoteRagHistoryTurn;

import java.util.List;

/**
 * 笔记 RAG 检索服务。
 */
public interface INoteRagRetrieveService {

    /**
     * 检索平台公开正常笔记，返回可用于问答的父分片上下文。
     */
    default NoteRagRetrieveResult retrieve(String query) {
        return retrieve(query, List.of());
    }

    NoteRagRetrieveResult retrieve(String query, List<NoteRagHistoryTurn> history);
}
