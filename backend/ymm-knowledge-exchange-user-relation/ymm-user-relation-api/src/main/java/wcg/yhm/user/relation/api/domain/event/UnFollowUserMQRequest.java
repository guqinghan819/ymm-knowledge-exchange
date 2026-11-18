package wcg.yhm.user.relation.api.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnFollowUserMQRequest {
    private String operationId;
    private Long userId;
    private Long unfollowUserId;
    private LocalDateTime createTime;
}
