package wcg.yhm.ai.biz.service;

import reactor.core.publisher.Flux;
import wcg.yhm.ai.biz.domain.request.NoteRagChatRequest;

/**
 * 笔记 RAG 问答服务。
 */
public interface INoteRagChatService {

    /**
     * 流式执行笔记 RAG 问答。
     */
    Flux<String> chatStream(NoteRagChatRequest request);
}
