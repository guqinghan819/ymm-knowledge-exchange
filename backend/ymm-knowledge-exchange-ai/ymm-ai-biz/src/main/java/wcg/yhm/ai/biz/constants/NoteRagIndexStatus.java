package wcg.yhm.ai.biz.constants;

/**
 * 笔记 RAG 索引状态。
 */
public final class NoteRagIndexStatus {

    private NoteRagIndexStatus() {
    }

    public static final int CHUNKED = 1;
    public static final int VECTORIZED = 2;
    public static final int DELETED = 3;
    public static final int VECTORIZE_FAILED = 4;
}
