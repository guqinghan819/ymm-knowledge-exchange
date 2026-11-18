package wcg.yhm.ai.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wcg.yhm.ai.biz.domain.model.NoteRagCandidate;
import wcg.yhm.ai.biz.domain.model.NoteRagEvidenceDecision;
import wcg.yhm.ai.biz.service.INoteRagEvidenceService;

import java.util.ArrayList;
import java.util.List;

/** 严格证据门禁：服务异常或 JSON 无法解析时一律拒答。 */
@Slf4j
@Service
public class NoteRagEvidenceServiceImpl implements INoteRagEvidenceService {

    @Resource
    private ChatModel chatModel;

    @Override
    public NoteRagEvidenceDecision evaluate(String query, List<NoteRagCandidate> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return NoteRagEvidenceDecision.reject("没有召回到平台笔记证据");
        }
        try {
            StringBuilder evidence = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                evidence.append('[').append(i + 1).append("]\n")
                        .append(candidates.get(i).documentText()).append("\n\n");
            }
            String prompt = """
                    你是严格的证据充分性判定器。只能检查候选笔记文本是否足以回答问题，禁止使用自身知识补充。
                    主题相似但没有给出问题所需事实、条件或结论时，answerable 必须为 false。
                    只输出 JSON：{"answerable":true/false,"evidenceIndexes":[1,2],"reason":"简短原因"}。
                    evidenceIndexes 只能使用候选编号；只选择真正用于回答的证据。

                    问题：%s
                    候选：
                    %s
                    """.formatted(query, evidence);
            JSONObject object = JSON.parseObject(extractObject(chatModel.chat(prompt)));
            if (!object.containsKey("answerable")) {
                return NoteRagEvidenceDecision.reject("证据判定输出缺少answerable");
            }
            boolean answerable = object.getBooleanValue("answerable");
            List<Integer> indexes = object.getList("evidenceIndexes", Integer.class);
            List<Integer> valid = new ArrayList<>();
            if (indexes != null) {
                indexes.stream().filter(i -> i != null && i >= 1 && i <= candidates.size())
                        .distinct().forEach(valid::add);
            }
            if (!answerable || valid.isEmpty()) {
                return NoteRagEvidenceDecision.reject(StrUtil.blankToDefault(object.getString("reason"), "候选证据不足"));
            }
            return NoteRagEvidenceDecision.builder().answerable(true).evidenceIndexes(valid)
                    .reason(object.getString("reason")).build();
        } catch (Exception e) {
            log.warn("证据判定异常，按严格策略拒答: query={}", query, e);
            return NoteRagEvidenceDecision.reject("证据判定服务异常");
        }
    }

    private String extractObject(String response) {
        String value = StrUtil.nullToEmpty(response).trim();
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        return start >= 0 && end >= start ? value.substring(start, end + 1) : value;
    }
}
