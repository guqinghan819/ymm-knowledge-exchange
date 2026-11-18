package wcg.yhm.ai.biz.service;

import wcg.yhm.ai.biz.domain.pojo.AiNoteRagChildChunk;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagParentChunk;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;

import java.util.List;

/**
 * 笔记 RAG 向量服务。
 */
public interface INoteRagVectorService {

    /**
     * 删除指定笔记当前有效子分片对应的向量。
     */
    void deleteActiveVectors(Long noteId);

    /** 删除一组稳定 point ID。 */
    void deleteVectors(List<String> pointIds);

    /** 切换 Qdrant point 的有效代次标志。 */
    void setVectorsActive(List<String> pointIds, boolean active);

    /**
     * 将本次生成的子分片写入向量库，并同步更新分片状态。
     */
    void vectorize(NoteRagIndexDetailResponse note,
                   List<AiNoteRagParentChunk> parents,
                   List<AiNoteRagChildChunk> children);
}
