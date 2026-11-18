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
 * RAG 笔记父分片表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_note_rag_parent_chunk")
public class AiNoteRagParentChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 原始笔记 ID */
    private Long noteId;

    /** 父分片的稳定业务 ID */
    private String parentId;

    /** 同一次笔记索引构建的版本标识 */
    private String generationId;

    /** 是否属于当前生效的索引版本：0 否、1 是 */
    private Integer active;

    /** 父分片在笔记中的顺序 */
    private Integer parentIndex;

    /** 建立索引时的笔记标题快照 */
    private String title;

    /** 建立索引时的话题 ID 快照 */
    private Long topicId;

    /** 建立索引时的话题名称快照 */
    private String topicName;

    /** 笔记作者 ID 快照 */
    private Long creatorId;

    /** 笔记作者昵称快照 */
    private String creatorName;

    /** 笔记作者头像快照 */
    private String creatorAvatar;

    /** 笔记封面地址快照 */
    private String cover;

    /** 笔记类型快照 */
    private Integer type;

    /** 笔记可见范围快照 */
    private Integer visible;

    /** 笔记发布状态快照 */
    private Integer status;

    /** 原始正文在文本存储服务中的内容 ID */
    private String contentUuid;

    /** 完整正文的哈希值，用于检测内容变化 */
    private String contentHash;

    /** 该父分片文本的哈希值 */
    private String parentHash;

    /** 父分片在原始文本中的起始位置 */
    private Integer parentStart;

    /** 父分片在原始文本中的结束位置 */
    private Integer parentEnd;

    /** 父分片的文本长度 */
    private Integer parentSize;

    /** 父分片的完整文本 */
    private String parentText;

    /** 该父分片拆出的子分片数量 */
    private Integer childCount;

    /** 索引处理阶段，取值见 NoteRagIndexStatus */
    private Integer indexStatus;

    /** 分片索引失败时的错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    /** 分片的逻辑删除标记 */
    private Integer deleted;
}
