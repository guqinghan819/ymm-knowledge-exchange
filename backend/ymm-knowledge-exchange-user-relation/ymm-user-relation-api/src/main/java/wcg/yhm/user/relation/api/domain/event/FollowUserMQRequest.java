package wcg.yhm.user.relation.api.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowUserMQRequest {
    private String operationId;
    private Long userId;
    private Long followUserId;
    private LocalDateTime createTime;
}
