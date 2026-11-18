package wcg.yhm.ai.biz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wcg.yhm.ai.biz.domain.vo.NoteRagReferenceVO;

/**
 * 笔记 RAG 内部上下文，包含对外引用和用于提示词的父分片文本。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRagContext {

    private NoteRagReferenceVO reference;

    private String parentText;
}
