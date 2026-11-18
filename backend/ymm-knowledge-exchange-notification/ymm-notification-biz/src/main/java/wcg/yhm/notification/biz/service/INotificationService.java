package wcg.yhm.notification.biz.service;

import wcg.yhm.notification.api.domain.request.NotificationListRequest;
import wcg.yhm.notification.api.domain.response.NotificationItemResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;

public interface INotificationService {
    CursorPageResponse<NotificationItemResponse> list(NotificationListRequest request);

    Long unreadCount();

    void read(Long notificationId);

    void readAll();
}
