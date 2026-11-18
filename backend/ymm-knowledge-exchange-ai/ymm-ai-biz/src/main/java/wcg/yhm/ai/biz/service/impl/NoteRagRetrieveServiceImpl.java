package wcg.yhm.ai.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wcg.yhm.ai.biz.constants.NoteRagIndexStatus;
import wcg.yhm.ai.biz.domain.model.NoteRagCandidate;
import wcg.yhm.ai.biz.domain.model.NoteRagContext;
import wcg.yhm.ai.biz.domain.model.NoteRagEvidenceDecision;
import wcg.yhm.ai.biz.domain.model.NoteRagHistoryTurn;
import wcg.yhm.ai.biz.domain.model.NoteRagQueryPlan;
import wcg.yhm.ai.biz.domain.model.NoteRagRetrieveResult;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagChildChunk;
import wcg.yhm.ai.biz.domain.pojo.AiNoteRagParentChunk;
import wcg.yhm.ai.biz.domain.vo.NoteRagReferenceVO;
import wcg.yhm.ai.biz.mapper.AiNoteRagChildChunkMapper;
import wcg.yhm.ai.biz.mapper.AiNoteRagParentChunkMapper;
import wcg.yhm.ai.biz.service.INoteRagEvidenceService;
import wcg.yhm.ai.biz.service.INoteRagQueryRewriteService;
import wcg.yhm.ai.biz.service.INoteRagRerankService;
import wcg.yhm.ai.biz.service.INoteRagRetrieveService;
import wcg.yhm.ai.biz.service.NoteRagQdrantStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 查询规划 -> Qdrant 双路 RRF -> Rerank -> 严格证据门禁 -> 父分片扩展。 */
@Slf4j
@Service
public class NoteRagRetrieveServiceImpl implements INoteRagRetrieveService {

    @Resource
    private INoteRagQueryRewriteService queryPlanner;
    @Resource
    private NoteRagQdrantStore qdrantStore;
    @Resource
    private INoteRagRerankService rerankService;
    @Resource
    private INoteRagEvidenceService evidenceService;
    @Resource
    private AiNoteRagChildChunkMapper childChunkMapper;
    @Resource
    private AiNoteRagParentChunkMapper parentChunkMapper;

    @Value("${rag.note.evidence.candidate-count:8}")
    private int evidenceCandidateCount;
    @Value("${rag.note.context.max-parent-count:4}")
    private int maxParentCount;
    @Value("${rag.note.context.max-parent-per-note:2}")
    private int maxParentPerNote;
    @Value("${rag.note.context.max-chars:6000}")
    private int maxContextChars;

    @Override
    public NoteRagRetrieveResult retrieve(String query, List<NoteRagHistoryTurn> history) {
        String original = normalize(query);
        if (StrUtil.isBlank(original)) {
            return empty(original, "问题为空");
        }
        NoteRagQueryPlan plan = queryPlanner.plan(original, history == null ? List.of() : history);
        if (plan == null || StrUtil.isBlank(plan.getStandaloneQuery())) {
            plan = NoteRagQueryPlan.builder().standaloneQuery(original)
                    .exactTerms(List.of()).expandedQueries(List.of()).build();
        }
        List<String> semanticQueries = semanticQueries(plan);
        String sparseQuery = buildSparseQuery(plan);
        List<NoteRagCandidate> candidates = hydrate(qdrantStore.search(semanticQueries, sparseQuery));
        if (candidates.isEmpty()) {
            return result(original, plan, semanticQueries, false, "未召回到有效公开笔记", candidates, List.of());
        }

        List<NoteRagCandidate> ranked;
        try {
            ranked = rerankService.rerank(plan.getStandaloneQuery(), candidates);
        } catch (RuntimeException e) {
            log.warn("Rerank不可用，使用Qdrant RRF顺序继续证据判定: query={}", plan.getStandaloneQuery(), e);
            ranked = candidates;
        }
        List<NoteRagCandidate> gateCandidates = ranked.stream()
                .limit(Math.max(1, evidenceCandidateCount)).toList();
        NoteRagEvidenceDecision decision = evidenceService.evaluate(plan.getStandaloneQuery(), gateCandidates);
        if (decision == null || !decision.isAnswerable()) {
            String reason = decision == null ? "证据判定服务异常" : decision.getReason();
            return result(original, plan, semanticQueries, false, reason, ranked, List.of());
        }

        List<NoteRagCandidate> selected = decision.getEvidenceIndexes().stream()
                .filter(Objects::nonNull)
                .filter(index -> index >= 1 && index <= gateCandidates.size())
                .map(index -> gateCandidates.get(index - 1))
                .distinct().toList();
        List<NoteRagContext> contexts = buildContexts(selected, plan.getStandaloneQuery());
        if (contexts.isEmpty()) {
            return result(original, plan, semanticQueries, false, "证据对应的有效父分片不存在", ranked, List.of());
        }
        return result(original, plan, semanticQueries, true, decision.getReason(), ranked, contexts);
    }

    private List<NoteRagCandidate> hydrate(List<NoteRagCandidate> raw) {
        if (CollUtil.isEmpty(raw)) {
            return List.of();
        }
        List<String> pointIds = raw.stream().map(NoteRagCandidate::getPointId).distinct().toList();
        Map<String, AiNoteRagChildChunk> children = childChunkMapper.selectList(
                        Wrappers.<AiNoteRagChildChunk>lambdaQuery()
                                .in(AiNoteRagChildChunk::getPointId, pointIds)
                                .eq(AiNoteRagChildChunk::getActive, 1)
                                .eq(AiNoteRagChildChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZED)
                                .eq(AiNoteRagChildChunk::getDeleted, 0))
                .stream().collect(Collectors.toMap(AiNoteRagChildChunk::getPointId, Function.identity(), (a, b) -> a));
        Set<String> parentIds = children.values().stream().map(AiNoteRagChildChunk::getParentId).collect(Collectors.toSet());
        Map<String, AiNoteRagParentChunk> parents = parentIds.isEmpty() ? Map.of() : parentChunkMapper.selectList(
                        Wrappers.<AiNoteRagParentChunk>lambdaQuery()
                                .in(AiNoteRagParentChunk::getParentId, parentIds)
                                .eq(AiNoteRagParentChunk::getActive, 1)
                                .eq(AiNoteRagParentChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZED)
                                .eq(AiNoteRagParentChunk::getDeleted, 0))
                .stream().collect(Collectors.toMap(AiNoteRagParentChunk::getParentId, Function.identity(), (a, b) -> a));
        List<NoteRagCandidate> result = new ArrayList<>();
        for (NoteRagCandidate candidate : raw) {
            AiNoteRagChildChunk child = children.get(candidate.getPointId());
            AiNoteRagParentChunk parent = child == null ? null : parents.get(child.getParentId());
            if (child == null || parent == null || !Objects.equals(child.getGenerationId(), parent.getGenerationId())) {
                continue;
            }
            candidate.setChild(child);
            candidate.setParentId(child.getParentId());
            candidate.setNoteId(child.getNoteId());
            candidate.setTitle(parent.getTitle());
            candidate.setTopicName(parent.getTopicName());
            candidate.setCreatorName(parent.getCreatorName());
            candidate.setCover(parent.getCover());
            result.add(candidate);
        }
        return result;
    }

    private List<NoteRagContext> buildContexts(List<NoteRagCandidate> selected, String standaloneQuery) {
        if (CollUtil.isEmpty(selected)) {
            return List.of();
        }
        List<String> parentIds = selected.stream().map(NoteRagCandidate::getParentId).distinct().toList();
        Map<String, AiNoteRagParentChunk> parents = parentChunkMapper.selectList(
                        Wrappers.<AiNoteRagParentChunk>lambdaQuery()
                                .in(AiNoteRagParentChunk::getParentId, parentIds)
                                .eq(AiNoteRagParentChunk::getActive, 1)
                                .eq(AiNoteRagParentChunk::getIndexStatus, NoteRagIndexStatus.VECTORIZED)
                                .eq(AiNoteRagParentChunk::getDeleted, 0))
                .stream().collect(Collectors.toMap(AiNoteRagParentChunk::getParentId, Function.identity(), (a, b) -> a));
        Map<Long, Integer> perNote = new HashMap<>();
        Set<String> usedParents = new LinkedHashSet<>();
        List<NoteRagContext> contexts = new ArrayList<>();
        int remaining = Math.max(1, maxContextChars);
        for (NoteRagCandidate candidate : selected) {
            AiNoteRagParentChunk parent = parents.get(candidate.getParentId());
            if (parent == null || usedParents.contains(parent.getParentId())
                    || perNote.getOrDefault(parent.getNoteId(), 0) >= Math.max(1, maxParentPerNote)) {
                continue;
            }
            String text = StrUtil.nullToEmpty(parent.getParentText());
            if (StrUtil.isBlank(text) || remaining <= 0) {
                continue;
            }
            String contextText = text.length() <= remaining ? text : text.substring(0, remaining);
            int citationNo = contexts.size() + 1;
            contexts.add(NoteRagContext.builder().parentText(contextText)
                    .reference(buildReference(parent, candidate, standaloneQuery, citationNo)).build());
            remaining -= contextText.length();
            usedParents.add(parent.getParentId());
            perNote.merge(parent.getNoteId(), 1, Integer::sum);
            if (contexts.size() >= Math.max(1, maxParentCount)) {
                break;
            }
        }
        return contexts;
    }

    private NoteRagReferenceVO buildReference(AiNoteRagParentChunk parent, NoteRagCandidate candidate,
                                              String query, int citationNo) {
        return NoteRagReferenceVO.builder()
                .citationNo(citationNo)
                .noteId(parent.getNoteId()).parentId(parent.getParentId()).title(parent.getTitle())
                .topicName(parent.getTopicName()).creatorName(parent.getCreatorName()).cover(parent.getCover())
                .snippet(candidate.getChild() == null ? null : candidate.getChild().getSnippet())
                .score(candidate.getRerankScore() != null ? candidate.getRerankScore() : candidate.getRrfScore())
                .matchedQuery(query).build();
    }

    private List<String> semanticQueries(NoteRagQueryPlan plan) {
        Set<String> values = new LinkedHashSet<>();
        values.add(normalize(plan.getStandaloneQuery()));
        if (plan.getExpandedQueries() != null) {
            plan.getExpandedQueries().stream().map(this::normalize).filter(StrUtil::isNotBlank).limit(2).forEach(values::add);
        }
        return new ArrayList<>(values);
    }

    private String buildSparseQuery(NoteRagQueryPlan plan) {
        StringBuilder value = new StringBuilder(plan.getStandaloneQuery());
        if (plan.getExactTerms() != null && !plan.getExactTerms().isEmpty()) {
            value.append(" ").append(String.join(" ", plan.getExactTerms()));
        }
        return value.toString();
    }

    private NoteRagRetrieveResult empty(String query, String reason) {
        NoteRagQueryPlan plan = NoteRagQueryPlan.builder().standaloneQuery(query)
                .exactTerms(List.of()).expandedQueries(List.of()).build();
        return result(query, plan, List.of(), false, reason, List.of(), List.of());
    }

    private NoteRagRetrieveResult result(String query, NoteRagQueryPlan plan, List<String> queries,
                                         boolean answerable, String reason, List<NoteRagCandidate> candidates,
                                         List<NoteRagContext> contexts) {
        return NoteRagRetrieveResult.builder().query(query).queries(queries)
                .standaloneQuery(plan.getStandaloneQuery()).exactTerms(plan.getExactTerms())
                .answerable(answerable).evidenceReason(reason).candidates(candidates).contexts(contexts)
                .references(buildResultReferences(candidates, contexts, plan.getStandaloneQuery())).build();
    }

    private List<NoteRagReferenceVO> buildResultReferences(List<NoteRagCandidate> candidates,
                                                            List<NoteRagContext> contexts,
                                                            String matchedQuery) {
        if (CollUtil.isNotEmpty(contexts)) {
            return contexts.stream().map(NoteRagContext::getReference).filter(Objects::nonNull).toList();
        }
        if (CollUtil.isEmpty(candidates)) {
            return List.of();
        }
        Set<Long> usedNotes = new LinkedHashSet<>();
        List<NoteRagReferenceVO> references = new ArrayList<>();
        for (NoteRagCandidate candidate : candidates) {
            if (candidate.getNoteId() == null || !usedNotes.add(candidate.getNoteId())) {
                continue;
            }
            references.add(NoteRagReferenceVO.builder()
                    .citationNo(references.size() + 1)
                    .noteId(candidate.getNoteId())
                    .parentId(candidate.getParentId())
                    .title(candidate.getTitle())
                    .topicName(candidate.getTopicName())
                    .creatorName(candidate.getCreatorName())
                    .cover(candidate.getCover())
                    .snippet(candidate.getChild() == null ? null : candidate.getChild().getSnippet())
                    .score(candidate.getRerankScore() != null ? candidate.getRerankScore() : candidate.getRrfScore())
                    .matchedQuery(matchedQuery)
                    .build());
            if (references.size() >= Math.max(1, maxParentCount)) {
                break;
            }
        }
        return references;
    }

    private String normalize(String query) {
        return StrUtil.nullToEmpty(query).replaceAll("[\\r\\n\\t]+", " ").trim();
    }
}
