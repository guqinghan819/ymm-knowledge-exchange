package wcg.yhm.note.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 与笔记写入同一事务保存的 RAG 索引同步事件，失败后由发布器重试。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("note_rag_outbox")
public class NoteRagOutbox {
    /** 数据库主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** RAG 同步事件的唯一标识，供消费端去重 */
    private String eventId;
    /** 关联的笔记 ID */
    private Long noteId;
    /** RAG 索引变更类型：新增或删除 */
    private String eventType;
    /** 触发本次索引同步时的笔记更新时间 */
    private LocalDateTime noteUpdatedAt;
    /** 事件消息结构版本 */
    private Integer schemaVersion;
    /** 投递状态：0 待投递、1 已投递、2 失败待重试 */
    private Integer status;
    /** 消息投递失败后的累计重试次数 */
    private Integer attempts;
    /** 下次允许重试的时间 */
    private LocalDateTime nextRetryTime;
    /** 最近一次消息投递失败的原因 */
    private String errorMessage;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
