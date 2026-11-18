package wcg.yhm.notification.api.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class NotificationListRequest {
    private Long cursor;

    @Min(1)
    @Max(50)
    private Integer limit;
}
