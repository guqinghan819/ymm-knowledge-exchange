package wcg.yhm.note.api.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏、取消收藏共享消息契约。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MQCollectUnCollectDTO {
    /**
     * 消息幂等标识；旧消息可能为空。
     */
    private String operationId;

    private Long userId;

    private Long noteId;

    /**
     * 1：收藏，0：取消收藏。
     */
    private Integer status;

    private LocalDateTime createTime;

    /**
     * 笔记作者 ID，也是通知接收者 ID。
     */
    private Long creatorId;
}
