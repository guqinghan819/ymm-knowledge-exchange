package wcg.yhm.ai.biz.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QueryFactory;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import wcg.yhm.ai.biz.domain.model.NoteRagCandidate;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagChildChunk;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Qdrant V2 双路（稠密向量 + 原生 BM25 稀疏向量）存储与 RRF 召回。 */
@Slf4j
@Component
public class NoteRagQdrantStore {

    private static final String BM25_MODEL = "qdrant/bm25";

    @Resource
    private QdrantClient qdrantClient;

    @Resource
    private EmbeddingModel noteRagEmbeddingModel;

    @Value("${rag.note.vector.qdrant.collection-name:note_rag_child_chunk_v2}")
    private String collectionName;

    @Value("${rag.note.vector.qdrant.dense-vector-name:dense}")
    private String denseVectorName;

    @Value("${rag.note.vector.qdrant.sparse-vector-name:sparse}")
    private String sparseVectorName;

    @Value("${rag.note.retrieve.dense-top-k:20}")
    private int denseTopK;

    @Value("${rag.note.retrieve.sparse-top-k:30}")
    private int sparseTopK;

    @Value("${rag.note.retrieve.candidate-limit:40}")
    private int candidateLimit;

    @Value("${rag.note.retrieve.max-child-per-note:3}")
    private int maxChildPerNote;

    @Value("${rag.note.retrieve.rrf-k:60}")
    private int rrfK;

    @Value("${rag.note.retrieve.hybrid-enabled:true}")
    private boolean hybridEnabled;

    @Value("${rag.note.vector.qdrant.timeout-seconds:10}")
    private int timeoutSeconds;

    public void upsert(NoteRagIndexDetailResponse note, List<AiNoteRagChildChunk> children) {
        if (note == null || CollUtil.isEmpty(children)) {
            return;
        }
        List<Points.PointStruct> points = new ArrayList<>(children.size());
        for (AiNoteRagChildChunk child : children) {
            String indexedText = indexedText(note, child.getChildText());
            Embedding dense = noteRagEmbeddingModel.embed(indexedText).content();
            Points.Document sparse = sparseDocument(sparseIndexedText(note, child.getChildText()));
            Map<String, Points.Vector> vectors = new HashMap<>();
            vectors.put(denseVectorName, VectorFactory.vector(dense.vectorAsList()));
            vectors.put(sparseVectorName, VectorFactory.vector(sparse));

            Map<String, JsonWithInt.Value> payload = new HashMap<>();
            payload.put("pointId", ValueFactory.value(child.getPointId()));
            payload.put("noteId", ValueFactory.value(child.getNoteId()));
            payload.put("parentId", ValueFactory.value(child.getParentId()));
            payload.put("generationId", ValueFactory.value(StrUtil.nullToEmpty(child.getGenerationId())));
            payload.put("active", ValueFactory.value(false));
            payload.put("visible", ValueFactory.value(note.getVisible().longValue()));
            payload.put("status", ValueFactory.value(note.getStatus().longValue()));
            payload.put("title", ValueFactory.value(StrUtil.nullToEmpty(note.getTitle())));
            payload.put("topicName", ValueFactory.value(StrUtil.nullToEmpty(note.getTopicName())));
            payload.put("childText", ValueFactory.value(child.getChildText()));

            points.add(Points.PointStruct.newBuilder()
                    .setId(PointIdFactory.id(UUID.fromString(child.getPointId())))
                    .setVectors(VectorsFactory.namedVectors(vectors))
                    .putAllPayload(payload)
                    .build());
        }
        await(qdrantClient.upsertAsync(collectionName, points, timeout()), "批量写入Qdrant失败");
    }

    public void deleteByPointIds(List<String> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return;
        }
        List<Common.PointId> ids = pointIds.stream()
                .filter(StrUtil::isNotBlank)
                .map(UUID::fromString)
                .map(PointIdFactory::id)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        await(qdrantClient.deleteAsync(collectionName, ids, timeout()), "删除Qdrant分片失败");
    }

    public void setActive(List<String> pointIds, boolean active) {
        if (CollUtil.isEmpty(pointIds)) {
            return;
        }
        List<Common.PointId> ids = pointIds.stream().filter(StrUtil::isNotBlank)
                .map(UUID::fromString).map(PointIdFactory::id).toList();
        Points.PointsIdsList idList = Points.PointsIdsList.newBuilder().addAllIds(ids).build();
        Points.SetPayloadPoints request = Points.SetPayloadPoints.newBuilder()
                .setCollectionName(collectionName).setWait(true)
                .putPayload("active", ValueFactory.value(active))
                .setPointsSelector(Points.PointsSelector.newBuilder().setPoints(idList))
                .build();
        await(qdrantClient.setPayloadAsync(request, timeout()), "更新Qdrant generation状态失败");
    }

    public List<NoteRagCandidate> search(List<String> semanticQueries, String sparseQuery) {
        if (CollUtil.isEmpty(semanticQueries)) {
            return List.of();
        }
        try {
            if (hybridEnabled && StrUtil.isNotBlank(sparseQuery)) {
                return query(semanticQueries, sparseQuery, true);
            }
        } catch (RuntimeException e) {
            log.warn("Qdrant稠密+稀疏召回失败，降级为纯稠密召回", e);
        }
        return query(semanticQueries, null, false);
    }

    private List<NoteRagCandidate> query(List<String> semanticQueries, String sparseQuery, boolean includeSparse) {
        List<Points.PrefetchQuery> prefetches = new ArrayList<>();
        Common.Filter activeFilter = Common.Filter.newBuilder()
                .addMust(ConditionFactory.match("active", true))
                .addMust(ConditionFactory.match("visible", 0L))
                .addMust(ConditionFactory.match("status", 1L))
                .build();
        semanticQueries.stream().filter(StrUtil::isNotBlank).distinct().limit(3).forEach(query -> {
            Embedding embedding = noteRagEmbeddingModel.embed(query).content();
            prefetches.add(Points.PrefetchQuery.newBuilder()
                    .setQuery(QueryFactory.nearest(embedding.vectorAsList()))
                    .setUsing(denseVectorName)
                    .setFilter(activeFilter)
                    .setLimit(Math.max(1, denseTopK))
                    .build());
        });
        if (includeSparse) {
            prefetches.add(Points.PrefetchQuery.newBuilder()
                    .setQuery(QueryFactory.nearest(sparseDocument(sparseQuery)))
                    .setUsing(sparseVectorName)
                    .setFilter(activeFilter)
                    .setLimit(Math.max(1, sparseTopK))
                    .build());
        }
        if (prefetches.isEmpty()) {
            return List.of();
        }

        Points.Rrf rrf = Points.Rrf.newBuilder().setK(Math.max(1, rrfK)).build();
        Points.QueryPoints request = Points.QueryPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllPrefetch(prefetches)
                .setQuery(QueryFactory.rrf(rrf))
                .setLimit(Math.max(1, candidateLimit))
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .build();
        List<Points.ScoredPoint> points = await(qdrantClient.queryAsync(request, timeout()), "Qdrant召回失败");
        return capPerNote(points, includeSparse ? "qdrant_hybrid_rrf" : "qdrant_dense_rrf");
    }

    private List<NoteRagCandidate> capPerNote(List<Points.ScoredPoint> points, String source) {
        if (CollUtil.isEmpty(points)) {
            return List.of();
        }
        Map<Long, Integer> noteCounts = new LinkedHashMap<>();
        List<NoteRagCandidate> result = new ArrayList<>();
        int rawRank = 0;
        for (Points.ScoredPoint point : points) {
            rawRank++;
            Long noteId = longPayload(point, "noteId");
            String parentId = stringPayload(point, "parentId");
            String pointId = stringPayload(point, "pointId");
            if (noteId == null || StrUtil.isBlank(parentId) || StrUtil.isBlank(pointId)) {
                continue;
            }
            int count = noteCounts.getOrDefault(noteId, 0);
            if (count >= Math.max(1, maxChildPerNote)) {
                continue;
            }
            noteCounts.put(noteId, count + 1);
            result.add(NoteRagCandidate.builder()
                    .pointId(pointId)
                    .noteId(noteId)
                    .parentId(parentId)
                    .rrfScore((double) point.getScore())
                    .rrfRank(rawRank)
                    .recallSource(source)
                    .build());
            if (result.size() >= Math.max(1, candidateLimit)) {
                break;
            }
        }
        return result;
    }

    private Points.Document sparseDocument(String text) {
        return Points.Document.newBuilder()
                .setModel(BM25_MODEL)
                .setText(text)
                .putOptions("tokenizer", ValueFactory.value("multilingual"))
                .build();
    }

    private String indexedText(NoteRagIndexDetailResponse note, String childText) {
        return "标题：" + StrUtil.nullToEmpty(note.getTitle()) + "\n话题："
                + StrUtil.nullToEmpty(note.getTopicName()) + "\n正文：" + StrUtil.nullToEmpty(childText);
    }

    private String sparseIndexedText(NoteRagIndexDetailResponse note, String childText) {
        String title = StrUtil.nullToEmpty(note.getTitle());
        String topic = StrUtil.nullToEmpty(note.getTopicName());
        // Qdrant 稀疏向量没有 ES multi_match 字段权重，重复字段文本近似 title^3/topic^2 的词频权重。
        return title + " " + title + " " + title + " " + topic + " " + topic + " "
                + StrUtil.nullToEmpty(childText);
    }

    private Long longPayload(Points.ScoredPoint point, String name) {
        JsonWithInt.Value value = point.getPayloadMap().get(name);
        return value != null && value.hasIntegerValue() ? value.getIntegerValue() : null;
    }

    private String stringPayload(Points.ScoredPoint point, String name) {
        JsonWithInt.Value value = point.getPayloadMap().get(name);
        return value != null && value.hasStringValue() ? value.getStringValue() : null;
    }

    private Duration timeout() {
        return Duration.ofSeconds(Math.max(1, timeoutSeconds));
    }

    private <T> T await(java.util.concurrent.Future<T> future, String message) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(message + "（线程中断）", e);
        } catch (Exception e) {
            throw new IllegalStateException(message, e);
        }
    }
}
