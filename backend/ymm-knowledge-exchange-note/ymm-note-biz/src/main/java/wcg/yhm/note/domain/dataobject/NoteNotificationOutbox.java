package wcg.yhm.note.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 与笔记状态变更同事务保存的通知事件，避免数据库已提交但消息丢失。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("note_notification_outbox")
public class NoteNotificationOutbox {
    /** 数据库主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 通知事件的唯一标识，供消费端幂等去重 */
    private String eventId;
    /** 通知业务事件类型 */
    private String eventType;
    /** 关联的笔记 ID */
    private Long noteId;
    /** 接收通知的用户 ID */
    private Long recipientId;
    /** 触发通知时的笔记标题快照 */
    private String noteTitle;
    /** 下架原因分类编码 */
    private String reasonCode;
    /** 可展示给用户的下架原因 */
    private String reason;
    /** 业务事件实际发生时间 */
    private LocalDateTime occurredAt;
    /** 事件消息结构版本 */
    private Integer schemaVersion;
    /** 通知事件的投递状态 */
    private Integer status;
    /** 通知事件投递失败后的累计重试次数 */
    private Integer attempts;
    /** 下次允许重试的时间 */
    private LocalDateTime nextRetryTime;
    /** 最近一次通知投递失败的原因 */
    private String errorMessage;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
