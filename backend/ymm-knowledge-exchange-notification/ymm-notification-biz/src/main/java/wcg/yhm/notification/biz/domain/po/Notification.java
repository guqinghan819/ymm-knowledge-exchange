package wcg.yhm.notification.biz.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 面向用户展示的通知记录；撤销和已读是两个独立状态。 */
@Data
@Accessors(chain = true)
@TableName("notification")
public class Notification {
    /** 通知记录主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 来源事件唯一标识，供通知幂等处理 */
    private String eventId;
    /** 通知接收者的用户 ID */
    private Long recipientId;
    /** 触发通知的用户 ID */
    private Long actorId;
    /** 通知类型：关注、笔记点赞、收藏或下架 */
    private Integer type;
    /** 通知指向的笔记或其他业务对象 ID */
    private Long targetId;
    /** 通知目标的标题快照 */
    private String targetTitle;
    /** 笔记下架原因分类编码 */
    private String reasonCode;
    /** 供接收者阅读的下架原因 */
    private String reason;
    /** 通知状态：0 已撤销、1 生效中 */
    private Integer status;
    /** 接收者是否已读 */
    private Boolean isRead;
    /** 通知创建时间 */
    private LocalDateTime createTime;
    /** 通知最后更新时间 */
    private LocalDateTime updateTime;
    /** 接收者阅读通知的时间 */
    private LocalDateTime readTime;
    /** 通知被撤销的时间 */
    private LocalDateTime revokeTime;
}
