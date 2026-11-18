package wcg.yhm.ai.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wcg.yhm.ai.biz.constants.NoteRagIndexStatus;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagChildChunk;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagParentChunk;
import wcg.yhm.ai.biz.mapper.AiNoteRagChildChunkMapper;
import wcg.yhm.ai.biz.mapper.AiNoteRagParentChunkMapper;
import wcg.yhm.ai.biz.service.INoteRagVectorService;
import wcg.yhm.ai.biz.service.NoteRagQdrantStore;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** 笔记 RAG V2 稠密/稀疏双向量写入。 */
@Slf4j
@Service
public class NoteRagVectorServiceImpl implements INoteRagVectorService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 512;

    @Resource
    private NoteRagQdrantStore qdrantStore;
    @Resource
    private AiNoteRagParentChunkMapper parentChunkMapper;
    @Resource
    private AiNoteRagChildChunkMapper childChunkMapper;

    @Override
    public void deleteActiveVectors(Long noteId) {
        if (Objects.isNull(noteId) || noteId <= 0) {
            return;
        }
        List<String> pointIds = childChunkMapper.selectList(Wrappers.<AiNoteRagChildChunk>lambdaQuery()
                        .select(AiNoteRagChildChunk::getPointId)
                        .eq(AiNoteRagChildChunk::getNoteId, noteId)
                        .eq(AiNoteRagChildChunk::getActive, 1)
                        .eq(AiNoteRagChildChunk::getDeleted, 0))
                .stream().map(AiNoteRagChildChunk::getPointId).filter(StrUtil::isNotBlank).distinct().toList();
        deleteVectors(pointIds);
    }

    @Override
    public void deleteVectors(List<String> pointIds) {
        qdrantStore.deleteByPointIds(pointIds);
    }

    @Override
    public void setVectorsActive(List<String> pointIds, boolean active) {
        qdrantStore.setActive(pointIds, active);
    }

    @Override
    public void vectorize(NoteRagIndexDetailResponse note,
                          List<AiNoteRagParentChunk> parents,
                          List<AiNoteRagChildChunk> children) {
        if (CollUtil.isEmpty(children)) {
            return;
        }
        try {
            qdrantStore.upsert(note, children);
            markVectorized(parents, children);
            log.info("RAG V2双向量写入完成: noteId={}, generationId={}, childChunks={}",
                    note.getNoteId(), children.get(0).getGenerationId(), children.size());
        } catch (RuntimeException e) {
            markFailed(parents, children, e);
            throw new IllegalStateException("RAG V2双向量写入失败: noteId=" + note.getNoteId(), e);
        }
    }

    private void markVectorized(List<AiNoteRagParentChunk> parents, List<AiNoteRagChildChunk> children) {
        LocalDateTime now = LocalDateTime.now();
        children.forEach(child -> {
            childChunkMapper.update(null, Wrappers.<AiNoteRagChildChunk>lambdaUpdate()
                    .eq(AiNoteRagChildChunk::getPointId, child.getPointId())
                    .eq(AiNoteRagChildChunk::getDeleted, 0)
                    .set(AiNoteRagChildChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZED)
                    .set(AiNoteRagChildChunk::getErrorMessage, null)
                    .set(AiNoteRagChildChunk::getUpdateTime, now));
            child.setIndexStatus(NoteRagIndexStatus.VECTORIZED);
        });
        parents.forEach(parent -> {
            parentChunkMapper.update(null, Wrappers.<AiNoteRagParentChunk>lambdaUpdate()
                    .eq(AiNoteRagParentChunk::getParentId, parent.getParentId())
                    .eq(AiNoteRagParentChunk::getDeleted, 0)
                    .set(AiNoteRagParentChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZED)
                    .set(AiNoteRagParentChunk::getErrorMessage, null)
                    .set(AiNoteRagParentChunk::getUpdateTime, now));
            parent.setIndexStatus(NoteRagIndexStatus.VECTORIZED);
        });
    }

    private void markFailed(List<AiNoteRagParentChunk> parents, List<AiNoteRagChildChunk> children, RuntimeException e) {
        String error = StrUtil.sub(StrUtil.blankToDefault(e.getMessage(), e.getClass().getSimpleName()),
                0, ERROR_MESSAGE_MAX_LENGTH);
        LocalDateTime now = LocalDateTime.now();
        children.forEach(child -> childChunkMapper.update(null, Wrappers.<AiNoteRagChildChunk>lambdaUpdate()
                .eq(AiNoteRagChildChunk::getPointId, child.getPointId())
                .eq(AiNoteRagChildChunk::getDeleted, 0)
                .set(AiNoteRagChildChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZE_FAILED)
                .set(AiNoteRagChildChunk::getErrorMessage, error)
                .set(AiNoteRagChildChunk::getUpdateTime, now)));
        parents.forEach(parent -> parentChunkMapper.update(null, Wrappers.<AiNoteRagParentChunk>lambdaUpdate()
                .eq(AiNoteRagParentChunk::getParentId, parent.getParentId())
                .eq(AiNoteRagParentChunk::getDeleted, 0)
                .set(AiNoteRagParentChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZE_FAILED)
                .set(AiNoteRagParentChunk::getErrorMessage, error)
                .set(AiNoteRagParentChunk::getUpdateTime, now)));
    }
}
