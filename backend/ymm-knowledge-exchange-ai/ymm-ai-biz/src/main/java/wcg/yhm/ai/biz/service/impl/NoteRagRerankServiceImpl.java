package wcg.yhm.ai.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wcg.yhm.ai.biz.domain.model.NoteRagCandidate;
import wcg.yhm.ai.biz.service.INoteRagRerankService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** 阿里云 qwen3-rerank HTTP 精排。异常由调用方降级为 RRF。 */
@Slf4j
@Service
public class NoteRagRerankServiceImpl implements INoteRagRerankService {

    private static final String INSTRUCT =
            "Given a web search query, retrieve relevant passages that answer the query.";

    @Value("${rag.note.rerank.enabled:true}")
    private boolean enabled;
    @Value("${rag.note.rerank.endpoint:${DASHSCOPE_RERANK_ENDPOINT:}}")
    private String endpoint;
    @Value("${rag.note.rerank.workspace:${DASHSCOPE_WORKSPACE:}}")
    private String workspace;
    @Value("${rag.note.rerank.region:cn-beijing}")
    private String region;
    @Value("${rag.note.rerank.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;
    @Value("${rag.note.rerank.model:qwen3-rerank}")
    private String model;
    @Value("${rag.note.rerank.top-n:12}")
    private int topN;
    @Value("${rag.note.rerank.connect-timeout-ms:1500}")
    private int connectTimeoutMs;
    @Value("${rag.note.rerank.read-timeout-ms:3000}")
    private int readTimeoutMs;

    @Override
    public List<NoteRagCandidate> rerank(String query, List<NoteRagCandidate> candidates) {
        if (!enabled || CollUtil.isEmpty(candidates)) {
            return candidates;
        }
        String requestEndpoint = resolveEndpoint();
        if (StrUtil.isBlank(requestEndpoint) || StrUtil.isBlank(apiKey)) {
            throw new IllegalStateException("Rerank endpoint/workspace或DASHSCOPE_API_KEY未配置");
        }
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("query", query);
        body.put("documents", candidates.stream().map(NoteRagCandidate::documentText).toList());
        body.put("top_n", Math.min(Math.max(1, topN), candidates.size()));
        body.put("instruct", INSTRUCT);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(100, connectTimeoutMs))).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(requestEndpoint))
                    .timeout(Duration.ofMillis(Math.max(100, readTimeoutMs)))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Rerank HTTP " + response.statusCode());
            }
            return parseResults(response.body(), candidates);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Rerank调用被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("Rerank调用失败", e);
        }
    }

    List<NoteRagCandidate> parseResults(String responseBody, List<NoteRagCandidate> candidates) {
        JSONArray results = JSON.parseObject(responseBody).getJSONArray("results");
        if (results == null || results.isEmpty()) {
            throw new IllegalStateException("Rerank返回结果为空");
        }
        List<NoteRagCandidate> sorted = new ArrayList<>();
        for (int rank = 0; rank < results.size(); rank++) {
            JSONObject item = results.getJSONObject(rank);
            int index = item.getIntValue("index", -1);
            if (index < 0 || index >= candidates.size()) {
                throw new IllegalStateException("Rerank结果索引越界: " + index);
            }
            NoteRagCandidate candidate = candidates.get(index);
            candidate.setRerankRank(rank + 1);
            candidate.setRerankScore(item.getDouble("relevance_score"));
            sorted.add(candidate);
        }
        return sorted;
    }

    String resolveEndpoint() {
        if (StrUtil.isNotBlank(endpoint)) {
            return endpoint.trim();
        }
        if (StrUtil.isBlank(workspace)) {
            return "";
        }
        String selectedRegion = StrUtil.blankToDefault(region, "cn-beijing").trim();
        return "https://" + workspace.trim() + "." + selectedRegion
                + ".maas.aliyuncs.com/compatible-api/v1/reranks";
    }
}
