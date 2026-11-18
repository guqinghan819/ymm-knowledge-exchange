package wcg.yhm.notification.biz.domain.bo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationMutationResult {
    private Long recipientId;
    private Long addedNotificationId;
    @Builder.Default
    private List<Long> removedNotificationIds = List.of();
    private boolean duplicate;
}
