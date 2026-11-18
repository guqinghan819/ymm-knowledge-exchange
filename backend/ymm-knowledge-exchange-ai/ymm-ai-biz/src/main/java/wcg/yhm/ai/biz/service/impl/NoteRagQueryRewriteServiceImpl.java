package wcg.yhm.ai.biz.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wcg.yhm.ai.biz.domain.model.NoteRagHistoryTurn;
import wcg.yhm.ai.biz.domain.model.NoteRagQueryPlan;
import wcg.yhm.ai.biz.service.INoteRagQueryRewriteService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 会话指代消解、精确词抽取和语义扩展一次完成。 */
@Slf4j
@Service
public class NoteRagQueryRewriteServiceImpl implements INoteRagQueryRewriteService {

    private static final int KEYWORD_QUERY_MAX_LENGTH = 40;
    private static final String QUESTION_INTENT_PATTERN =
            ".*(什么|怎么|怎样|如何|为什么|为何|哪|谁|多少|是否|能否|可以|介绍|讲讲|说说|解释|含义|定义|区别|对比|优缺点|作用|原因|推荐|选择|吗|呢|么|[?？]).*";

    @Resource
    private ChatModel chatModel;

    @Override
    public NoteRagQueryPlan plan(String query, List<NoteRagHistoryTurn> history) {
        String original = normalize(query);
        if (StrUtil.isBlank(original)) {
            return fallback(original);
        }
        try {
            JSONObject object = JSON.parseObject(extractObject(chatModel.chat(buildPrompt(original, history))));
            String standalone = normalize(object.getString("standaloneQuery"));
            if (StrUtil.isBlank(standalone)) {
                standalone = original;
            }
            standalone = ensureAnswerableIntent(standalone);
            return NoteRagQueryPlan.builder()
                    .standaloneQuery(standalone)
                    .exactTerms(normalizeList(object.getList("exactTerms", String.class), null, 12))
                    .expandedQueries(normalizeList(object.getList("expandedQueries", String.class), standalone, 2))
                    .build();
        } catch (Exception e) {
            log.warn("笔记RAG查询规划失败，降级使用原问题: query={}", original, e);
            return fallback(original);
        }
    }

    private String buildPrompt(String query, List<NoteRagHistoryTurn> history) {
        String historyJson = JSON.toJSONString(history == null ? List.of() : history.stream().limit(3).toList());
        return """
                你是平台笔记检索的查询规划器。历史仅用于消解当前问题里的指代，绝不能作为答案证据。
                输出且只输出 JSON 对象：
                {"standaloneQuery":"可独立理解的当前问题","exactTerms":["型号/金额/数字/错误码/专名"],"expandedQueries":["语义扩展1","语义扩展2"]}
                要求：保持原意；standaloneQuery 补全“这个、第二个、那预算”等指代；精确词原样保留；扩展最多2个，不要引入新事实。

                最近会话：%s
                当前问题：%s
                """.formatted(historyJson, query);
    }

    private List<String> normalizeList(List<String> values, String excluded, int limit) {
        if (values == null) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (StrUtil.isBlank(normalized) || StrUtil.equals(normalized, excluded)) {
                continue;
            }
            result.add(normalized);
            if (result.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(result);
    }

    private String extractObject(String response) {
        String value = StrUtil.nullToEmpty(response).trim();
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        return start >= 0 && end >= start ? value.substring(start, end + 1) : value;
    }

    private String normalize(String query) {
        return StrUtil.nullToEmpty(query).replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private NoteRagQueryPlan fallback(String query) {
        return NoteRagQueryPlan.builder()
                .standaloneQuery(ensureAnswerableIntent(query))
                .exactTerms(List.of())
                .expandedQueries(List.of())
                .build();
    }

    /** 搜索框常传入单个主题词；补成开放式问题，避免证据门禁把“有召回”误判为“无资料”。 */
    private String ensureAnswerableIntent(String query) {
        String normalized = normalize(query);
        if (StrUtil.isBlank(normalized)
                || normalized.length() > KEYWORD_QUERY_MAX_LENGTH
                || normalized.matches(QUESTION_INTENT_PATTERN)) {
            return normalized;
        }
        return "请介绍一下“" + normalized + "”";
    }
}
