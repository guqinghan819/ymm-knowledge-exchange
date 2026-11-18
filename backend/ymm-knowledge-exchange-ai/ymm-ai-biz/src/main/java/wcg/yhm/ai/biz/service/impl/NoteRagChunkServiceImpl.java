package wcg.yhm.ai.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import wcg.yhm.ai.biz.constants.NoteRagIndexStatus;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagChildChunk;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagParentChunk;
import wcg.yhm.ai.biz.mapper.AiNoteRagChildChunkMapper;
import wcg.yhm.ai.biz.mapper.AiNoteRagParentChunkMapper;
import wcg.yhm.ai.biz.service.INoteRagChunkService;
import wcg.yhm.ai.biz.service.INoteRagVectorService;
import wcg.yhm.note.api.controller.NoteFeignApi;
import wcg.yhm.note.api.domain.request.QueryNoteRagIndexDetailRequest;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 笔记 RAG 父子分片与向量化编排实现。
 */
@Slf4j
@Service
public class NoteRagChunkServiceImpl implements INoteRagChunkService {

    private static final int PARENT_SIZE = 1500;
    private static final int PARENT_OVERLAP = 150;
    private static final int CHILD_SIZE = 450;
    private static final int CHILD_OVERLAP = 80;
    private static final int STATUS_NORMAL = 1;
    private static final int VISIBLE_PUBLIC = 0;

    @Resource
    private NoteFeignApi noteFeignApi;

    @Resource
    private AiNoteRagParentChunkMapper parentChunkMapper;

    @Resource
    private AiNoteRagChildChunkMapper childChunkMapper;

    @Resource
    private INoteRagVectorService noteRagVectorService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public void indexApprovedNote(Long noteId) {
        // 只有已发布、公开且有正文的笔记才进入检索库；其他状态清理旧索引。
        if (Objects.isNull(noteId) || noteId <= 0) {
            log.warn("RAG分片跳过，noteId非法: {}", noteId);
            return;
        }

        NoteRagIndexDetailResponse note = queryNoteDetail(noteId);
        if (!isIndexable(note)) {
            deleteVectorsAndSoftDeleteChunks(noteId);
            log.info("RAG分片跳过，笔记不可索引: noteId={}, status={}, visible={}, contentBlank={}",
                    noteId, note.getStatus(), note.getVisible(), StrUtil.isBlank(note.getContent()));
            return;
        }

        String content = normalizeContent(note.getContent());
        if (StrUtil.isBlank(content)) {
            deleteVectorsAndSoftDeleteChunks(noteId);
            log.info("RAG分片跳过，清洗后正文为空: noteId={}", noteId);
            return;
        }

        // 标题、话题和正文共同决定内容版本，相同版本不重复计算向量。
        String contentHash = DigestUtil.sha256Hex(StrUtil.nullToEmpty(note.getTitle()) + "\n"
                + StrUtil.nullToEmpty(note.getTopicName()) + "\n" + content);
        if (hasSameVectorizedVersion(noteId, contentHash)) {
            cleanupInactiveVectors(noteId);
            log.info("RAG索引跳过，正文hash未变化且已向量化: noteId={}, contentHash={}", noteId, contentHash);
            return;
        }

        List<ChunkWindow> parentWindows = split(content, PARENT_SIZE, PARENT_OVERLAP);
        if (CollUtil.isEmpty(parentWindows)) {
            deleteVectorsAndSoftDeleteChunks(noteId);
            log.info("RAG分片跳过，无有效父分片: noteId={}", noteId);
            return;
        }

        String generationId = stableId(noteId + ":" + contentHash + ":generation");
        LocalDateTime now = LocalDateTime.now();
        List<AiNoteRagParentChunk> parents = new ArrayList<>();
        List<AiNoteRagChildChunk> children = new ArrayList<>();
        int childIndex = 0;

        for (int i = 0; i < parentWindows.size(); i++) {
            ChunkWindow parentWindow = parentWindows.get(i);
            String parentId = stableId(noteId + ":" + contentHash + ":parent:" + i);
            List<ChunkWindow> childWindows = split(parentWindow.getText(), CHILD_SIZE, CHILD_OVERLAP);

            parents.add(buildParentChunk(note, parentWindow, i, parentId, generationId, contentHash,
                    childWindows.size(), now));

            for (ChunkWindow childWindow : childWindows) {
                int absoluteStart = parentWindow.getStart() + childWindow.getStart();
                int absoluteEnd = parentWindow.getStart() + childWindow.getEnd();
                String pointId = stableId(noteId + ":" + contentHash + ":child:" + childIndex);
                children.add(buildChildChunk(noteId, parentId, generationId, i, childIndex, pointId, childWindow.getText(),
                        absoluteStart, absoluteEnd, now));
                childIndex++;
            }
        }

        // 先保存未激活的新分片并完成向量化，再切换生效版本，避免检索到半成品。
        List<String> oldPointIds = selectActivePointIds(noteId);
        transactionTemplate.executeWithoutResult(status -> {
            deleteInactiveGeneration(generationId);
            parents.forEach(parentChunkMapper::insert);
            children.forEach(childChunkMapper::insert);
        });
        log.info("RAG分片完成，开始向量化: noteId={}, parentChunks={}, childChunks={}", noteId, parents.size(), children.size());
        noteRagVectorService.vectorize(note, parents, children);
        List<String> newPointIds = children.stream().map(AiNoteRagChildChunk::getPointId).toList();
        noteRagVectorService.setVectorsActive(newPointIds, true);
        transactionTemplate.executeWithoutResult(status -> activateGeneration(noteId, generationId));
        // 新版本可检索后再移除旧向量；清理失败可由后续任务重试。
        List<String> obsoletePointIds = oldPointIds.stream()
                .filter(pointId -> children.stream().noneMatch(child -> pointId.equals(child.getPointId())))
                .toList();
        try {
            noteRagVectorService.setVectorsActive(obsoletePointIds, false);
            noteRagVectorService.deleteVectors(obsoletePointIds);
        } catch (RuntimeException e) {
            log.warn("新generation已生效，异步清理可重试: noteId={}, generationId={}", noteId, generationId, e);
        }
    }

    private NoteRagIndexDetailResponse queryNoteDetail(Long noteId) {
        QueryNoteRagIndexDetailRequest request = new QueryNoteRagIndexDetailRequest(noteId);
        Response<NoteRagIndexDetailResponse> response = noteFeignApi.getRagIndexDetail(request);
        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            String message = Objects.nonNull(response) ? response.getMessage() : "empty response";
            throw new IllegalStateException("查询RAG索引笔记详情失败: noteId=" + noteId + ", message=" + message);
        }
        return response.getData();
    }

    private boolean isIndexable(NoteRagIndexDetailResponse note) {
        return Objects.nonNull(note)
                && Objects.equals(note.getStatus(), STATUS_NORMAL)
                && Objects.equals(note.getVisible(), VISIBLE_PUBLIC)
                && StrUtil.isNotBlank(note.getContent());
    }

    private boolean hasSameVectorizedVersion(Long noteId, String contentHash) {
        Long parentCount = parentChunkMapper.selectCount(Wrappers.<AiNoteRagParentChunk>lambdaQuery()
                .eq(AiNoteRagParentChunk::getNoteId, noteId)
                .eq(AiNoteRagParentChunk::getContentHash, contentHash)
                .eq(AiNoteRagParentChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZED)
                .eq(AiNoteRagParentChunk::getActive, 1)
                .eq(AiNoteRagParentChunk::getDeleted, 0));
        Long childCount = childChunkMapper.selectCount(Wrappers.<AiNoteRagChildChunk>lambdaQuery()
                .eq(AiNoteRagChildChunk::getNoteId, noteId)
                .eq(AiNoteRagChildChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZED)
                .eq(AiNoteRagChildChunk::getActive, 1)
                .eq(AiNoteRagChildChunk::getDeleted, 0));
        return parentCount != null && parentCount > 0 && childCount != null && childCount > 0;
    }

    private void deleteVectorsAndSoftDeleteChunks(Long noteId) {
        List<String> pointIds = selectAllPointIds(noteId);
        // 先让数据库中的generation失效，检索层会立即过滤，避免私密/删除内容在向量删除失败时继续服务。
        transactionTemplate.executeWithoutResult(status -> softDeleteOldChunks(noteId));
        noteRagVectorService.setVectorsActive(pointIds, false);
        noteRagVectorService.deleteVectors(pointIds);
    }

    private List<String> selectActivePointIds(Long noteId) {
        return childChunkMapper.selectList(Wrappers.<AiNoteRagChildChunk>lambdaQuery()
                        .select(AiNoteRagChildChunk::getPointId)
                        .eq(AiNoteRagChildChunk::getNoteId, noteId)
                        .eq(AiNoteRagChildChunk::getActive, 1)
                        .eq(AiNoteRagChildChunk::getDeleted, 0))
                .stream().map(AiNoteRagChildChunk::getPointId).filter(StrUtil::isNotBlank).distinct().toList();
    }

    private List<String> selectAllPointIds(Long noteId) {
        return childChunkMapper.selectList(Wrappers.<AiNoteRagChildChunk>lambdaQuery()
                        .select(AiNoteRagChildChunk::getPointId)
                        .eq(AiNoteRagChildChunk::getNoteId, noteId))
                .stream().map(AiNoteRagChildChunk::getPointId).filter(StrUtil::isNotBlank).distinct().toList();
    }

    private void cleanupInactiveVectors(Long noteId) {
        List<String> pointIds = childChunkMapper.selectList(Wrappers.<AiNoteRagChildChunk>lambdaQuery()
                        .select(AiNoteRagChildChunk::getPointId)
                        .eq(AiNoteRagChildChunk::getNoteId, noteId)
                        .and(wrapper -> wrapper.eq(AiNoteRagChildChunk::getActive, 0)
                                .or().eq(AiNoteRagChildChunk::getDeleted, 1)))
                .stream().map(AiNoteRagChildChunk::getPointId).filter(StrUtil::isNotBlank).distinct().toList();
        if (pointIds.isEmpty()) {
            return;
        }
        noteRagVectorService.setVectorsActive(pointIds, false);
        noteRagVectorService.deleteVectors(pointIds);
    }

    private void deleteInactiveGeneration(String generationId) {
        parentChunkMapper.delete(Wrappers.<AiNoteRagParentChunk>lambdaQuery()
                .eq(AiNoteRagParentChunk::getGenerationId, generationId)
                .eq(AiNoteRagParentChunk::getActive, 0));
        childChunkMapper.delete(Wrappers.<AiNoteRagChildChunk>lambdaQuery()
                .eq(AiNoteRagChildChunk::getGenerationId, generationId)
                .eq(AiNoteRagChildChunk::getActive, 0));
    }

    private void activateGeneration(Long noteId, String generationId) {
        LocalDateTime now = LocalDateTime.now();
        parentChunkMapper.update(null, Wrappers.<AiNoteRagParentChunk>lambdaUpdate()
                .eq(AiNoteRagParentChunk::getNoteId, noteId)
                .eq(AiNoteRagParentChunk::getActive, 1)
                .eq(AiNoteRagParentChunk::getDeleted, 0)
                .set(AiNoteRagParentChunk::getActive, 0)
                .set(AiNoteRagParentChunk::getDeleted, 1)
                .set(AiNoteRagParentChunk::getIndexStatus, NoteRagIndexStatus.DELETED)
                .set(AiNoteRagParentChunk::getUpdateTime, now));
        childChunkMapper.update(null, Wrappers.<AiNoteRagChildChunk>lambdaUpdate()
                .eq(AiNoteRagChildChunk::getNoteId, noteId)
                .eq(AiNoteRagChildChunk::getActive, 1)
                .eq(AiNoteRagChildChunk::getDeleted, 0)
                .set(AiNoteRagChildChunk::getActive, 0)
                .set(AiNoteRagChildChunk::getDeleted, 1)
                .set(AiNoteRagChildChunk::getIndexStatus, NoteRagIndexStatus.DELETED)
                .set(AiNoteRagChildChunk::getUpdateTime, now));
        parentChunkMapper.update(null, Wrappers.<AiNoteRagParentChunk>lambdaUpdate()
                .eq(AiNoteRagParentChunk::getGenerationId, generationId)
                .eq(AiNoteRagParentChunk::getDeleted, 0)
                .set(AiNoteRagParentChunk::getActive, 1)
                .set(AiNoteRagParentChunk::getUpdateTime, now));
        childChunkMapper.update(null, Wrappers.<AiNoteRagChildChunk>lambdaUpdate()
                .eq(AiNoteRagChildChunk::getGenerationId, generationId)
                .eq(AiNoteRagChildChunk::getDeleted, 0)
                .set(AiNoteRagChildChunk::getActive, 1)
                .set(AiNoteRagChildChunk::getUpdateTime, now));
    }

    private void softDeleteOldChunks(Long noteId) {
        LocalDateTime now = LocalDateTime.now();
        parentChunkMapper.update(null, Wrappers.<AiNoteRagParentChunk>lambdaUpdate()
                .eq(AiNoteRagParentChunk::getNoteId, noteId)
                .eq(AiNoteRagParentChunk::getDeleted, 0)
                .set(AiNoteRagParentChunk::getActive, 0)
                .set(AiNoteRagParentChunk::getDeleted, 1)
                .set(AiNoteRagParentChunk::getIndexStatus, NoteRagIndexStatus.DELETED)
                .set(AiNoteRagParentChunk::getUpdateTime, now));
        childChunkMapper.update(null, Wrappers.<AiNoteRagChildChunk>lambdaUpdate()
                .eq(AiNoteRagChildChunk::getNoteId, noteId)
                .eq(AiNoteRagChildChunk::getDeleted, 0)
                .set(AiNoteRagChildChunk::getActive, 0)
                .set(AiNoteRagChildChunk::getDeleted, 1)
                .set(AiNoteRagChildChunk::getIndexStatus, NoteRagIndexStatus.DELETED)
                .set(AiNoteRagChildChunk::getUpdateTime, now));
    }

    private AiNoteRagParentChunk buildParentChunk(NoteRagIndexDetailResponse note, ChunkWindow window,
                                                 int parentIndex, String parentId, String generationId, String contentHash,
                                                 int childCount, LocalDateTime now) {
        return AiNoteRagParentChunk.builder()
                .noteId(note.getNoteId())
                .parentId(parentId)
                .generationId(generationId)
                .active(0)
                .parentIndex(parentIndex)
                .title(note.getTitle())
                .topicId(note.getTopicId())
                .topicName(note.getTopicName())
                .creatorId(note.getCreatorId())
                .creatorName(note.getCreatorName())
                .creatorAvatar(note.getCreatorAvatar())
                .cover(note.getCover())
                .type(note.getType())
                .visible(note.getVisible())
                .status(note.getStatus())
                .contentUuid(note.getContentUuid())
                .contentHash(contentHash)
                .parentHash(DigestUtil.sha256Hex(window.getText()))
                .parentStart(window.getStart())
                .parentEnd(window.getEnd())
                .parentSize(window.getText().length())
                .parentText(window.getText())
                .childCount(childCount)
                .indexStatus(NoteRagIndexStatus.CHUNKED)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private AiNoteRagChildChunk buildChildChunk(Long noteId, String parentId, String generationId, int parentIndex,
                                               int childIndex, String pointId, String childText,
                                               int childStart, int childEnd, LocalDateTime now) {
        return AiNoteRagChildChunk.builder()
                .noteId(noteId)
                .parentId(parentId)
                .generationId(generationId)
                .active(0)
                .parentIndex(parentIndex)
                .pointId(pointId)
                .childIndex(childIndex)
                .childHash(DigestUtil.sha256Hex(childText))
                .childStart(childStart)
                .childEnd(childEnd)
                .childSize(childText.length())
                .childText(childText)
                .snippet(StrUtil.sub(childText, 0, 300))
                .indexStatus(NoteRagIndexStatus.CHUNKED)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private String normalizeContent(String content) {
        return StrUtil.nullToEmpty(content)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll(" {2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private List<ChunkWindow> split(String content, int chunkSize, int overlap) {
        List<ChunkWindow> result = new ArrayList<>();
        if (StrUtil.isBlank(content)) {
            return result;
        }

        int length = content.length();
        int start = 0;
        while (start < length) {
            int expectedEnd = Math.min(start + chunkSize, length);
            int end = expectedEnd < length ? findBackwardBoundary(content, start, expectedEnd, chunkSize) : expectedEnd;
            if (end <= start) {
                end = expectedEnd;
            }

            ChunkWindow window = buildWindow(content, start, end);
            if (StrUtil.isNotBlank(window.getText())) {
                result.add(window);
            }

            if (end >= length) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
            start = skipLeadingWhitespace(content, start);
        }
        return result;
    }

    private int findBackwardBoundary(String content, int start, int expectedEnd, int chunkSize) {
        int minEnd = Math.min(expectedEnd, start + Math.max(chunkSize / 2, chunkSize - 500));
        for (String boundaries : List.of("\n", "。！？!?；;", "，,、 ")) {
            int boundary = findBoundary(content, minEnd, expectedEnd, boundaries);
            if (boundary > 0) {
                return boundary;
            }
        }
        return expectedEnd;
    }

    private int findBoundary(String content, int minEnd, int expectedEnd, String boundaries) {
        for (int i = expectedEnd - 1; i >= minEnd; i--) {
            if (boundaries.indexOf(content.charAt(i)) >= 0) {
                return i + 1;
            }
        }
        return -1;
    }

    private ChunkWindow buildWindow(String content, int start, int end) {
        int adjustedStart = start;
        int adjustedEnd = end;
        while (adjustedStart < adjustedEnd && Character.isWhitespace(content.charAt(adjustedStart))) {
            adjustedStart++;
        }
        while (adjustedEnd > adjustedStart && Character.isWhitespace(content.charAt(adjustedEnd - 1))) {
            adjustedEnd--;
        }
        return new ChunkWindow(adjustedStart, adjustedEnd, content.substring(adjustedStart, adjustedEnd));
    }

    private int skipLeadingWhitespace(String content, int start) {
        int current = start;
        while (current < content.length() && Character.isWhitespace(content.charAt(current))) {
            current++;
        }
        return current;
    }

    private String stableId(String source) {
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Data
    @AllArgsConstructor
    private static class ChunkWindow {
        private int start;
        private int end;
        private String text;
    }
}
