package wcg.yhm.ai.biz.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 笔记子分片表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_note_rag_child_chunk")
public class AiNoteRagChildChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 原始笔记 ID */
    private Long noteId;

    /** 所属父分片的业务 ID */
    private String parentId;

    /** 同一次笔记索引构建的版本标识 */
    private String generationId;

    /** 是否属于当前生效的索引版本：0 否、1 是 */
    private Integer active;

    /** 所属父分片在笔记中的顺序 */
    private Integer parentIndex;

    /** 向量数据库中对应向量点的 ID */
    private String pointId;

    /** 子分片在父分片中的顺序 */
    private Integer childIndex;

    /** 子分片文本的哈希值 */
    private String childHash;

    /** 子分片在原始文本中的起始位置 */
    private Integer childStart;

    /** 子分片在原始文本中的结束位置 */
    private Integer childEnd;

    /** 子分片的文本长度 */
    private Integer childSize;

    /** 用于向量化的子分片文本 */
    private String childText;

    /** 检索结果展示用的简短摘录 */
    private String snippet;

    /** 向量索引处理阶段，取值见 NoteRagIndexStatus */
    private Integer indexStatus;

    /** 向量索引失败时的错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    /** 分片的逻辑删除标记 */
    private Integer deleted;
}
