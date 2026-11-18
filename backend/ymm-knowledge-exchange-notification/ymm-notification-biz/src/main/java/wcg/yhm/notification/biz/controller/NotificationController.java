package wcg.yhm.notification.biz.controller;

import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.notification.api.domain.request.NotificationListRequest;
import wcg.yhm.notification.api.domain.request.ReadNotificationRequest;
import wcg.yhm.notification.api.domain.response.NotificationItemResponse;
import wcg.yhm.notification.api.domain.response.UnreadCountResponse;
import wcg.yhm.notification.biz.service.INotificationService;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

@RestController
@RequestMapping("/notification")
public class NotificationController {
    @Resource
    private INotificationService notificationService;

    @PostMapping("/list")
    @ApiOperationLog(description = "游标查询互动通知")
    public CursorPageResponse<NotificationItemResponse> list(
            @Validated @RequestBody NotificationListRequest request) {
        return notificationService.list(request);
    }

    @GetMapping("/unread-count")
    @ApiOperationLog(description = "查询通知未读数")
    public Response<UnreadCountResponse> unreadCount() {
        return Response.success(new UnreadCountResponse(notificationService.unreadCount()));
    }

    @PostMapping("/read")
    @ApiOperationLog(description = "标记通知已读")
    public Response<?> read(@Validated @RequestBody ReadNotificationRequest request) {
        notificationService.read(request.getNotificationId());
        return Response.success();
    }

    @PostMapping("/read-all")
    @ApiOperationLog(description = "全部通知已读")
    public Response<?> readAll() {
        notificationService.readAll();
        return Response.success();
    }
}
