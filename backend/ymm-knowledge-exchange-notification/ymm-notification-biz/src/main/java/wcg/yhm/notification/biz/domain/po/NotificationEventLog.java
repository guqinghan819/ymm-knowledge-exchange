package wcg.yhm.notification.biz.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 记录已消费的事件 ID，防止 MQ 重复投递生成重复通知。 */
@Data
@Accessors(chain = true)
@TableName("notification_event_log")
public class NotificationEventLog {
    /** 事件处理日志主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 已处理事件的唯一标识，用于防止重复通知 */
    private String eventId;
    /** 已处理的通知事件类型 */
    private String eventType;
    /** 事件首次处理完成时间 */
    private LocalDateTime createTime;
}
