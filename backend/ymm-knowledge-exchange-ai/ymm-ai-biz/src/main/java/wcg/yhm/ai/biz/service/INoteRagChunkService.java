package wcg.yhm.ai.biz.service;

/**
 * 笔记 RAG 分片服务。
 */
public interface INoteRagChunkService {

    /**
     * 对审核通过的笔记执行分片落库。
     */
    void indexApprovedNote(Long noteId);
}
