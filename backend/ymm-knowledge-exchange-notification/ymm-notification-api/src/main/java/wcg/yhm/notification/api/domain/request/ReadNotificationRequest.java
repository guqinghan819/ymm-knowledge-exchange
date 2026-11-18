package wcg.yhm.notification.api.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadNotificationRequest {
    @NotNull
    private Long notificationId;
}
