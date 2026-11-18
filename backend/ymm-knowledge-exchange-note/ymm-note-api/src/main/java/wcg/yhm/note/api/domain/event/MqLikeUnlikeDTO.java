package wcg.yhm.note.api.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞、取消点赞共享消息契约。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqLikeUnlikeDTO {
    /**
     * 消息幂等标识；旧消息可能为空。
     */
    private String operationId;

    /**
     * 1：点赞，0：取消点赞。
     */
    private Integer type;

    private Long noteId;

    /**
     * 点赞操作用户 ID。
     */
    private Long userId;

    private LocalDateTime createTime;

    /**
     * 笔记作者 ID，也是通知接收者 ID。
     */
    private Long creatorId;
}
