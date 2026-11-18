package wcg.yhm.notification.api.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationItemResponse {
    private Long id;
    private Integer type;
    private Long actorId;
    private String actorNickname;
    private String actorAvatar;
    private Long noteId;
    private String noteTitle;
    private String reasonCode;
    private String reason;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createTime;
}
