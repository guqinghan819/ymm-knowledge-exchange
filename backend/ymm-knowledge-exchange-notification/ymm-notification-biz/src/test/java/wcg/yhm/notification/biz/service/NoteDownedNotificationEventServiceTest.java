package wcg.yhm.notification.biz.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.notification.biz.constants.NotificationConstants;
import wcg.yhm.notification.biz.domain.bo.NotificationMutationResult;
import wcg.yhm.notification.biz.domain.po.Notification;
import wcg.yhm.notification.biz.domain.po.NotificationEventLog;
import wcg.yhm.notification.biz.mapper.NotificationEventLogMapper;
import wcg.yhm.notification.biz.mapper.NotificationMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteDownedNotificationEventServiceTest {
    private NotificationMapper notificationMapper;
    private NotificationEventLogMapper eventLogMapper;
    private NotificationEventService service;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(Notification.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), Notification.class);
        }
        notificationMapper = mock(NotificationMapper.class);
        eventLogMapper = mock(NotificationEventLogMapper.class);
        service = new NotificationEventService();
        ReflectionTestUtils.setField(service, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(service, "eventLogMapper", eventLogMapper);
    }

    @Test
    void createsUnreadPlatformNotificationWithReasonSnapshot() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 15, 18, 0);
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(1);
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(99L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        NotificationMutationResult result = service.noteDowned(
                "down-1", 22L, 1001L, "测试笔记", "LOW_QUALITY", "内容低质", occurredAt);

        assertThat(result.getAddedNotificationId()).isEqualTo(99L);
        ArgumentCaptor<Notification> inserted = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(inserted.capture());
        assertThat(inserted.getValue())
                .extracting(Notification::getActorId, Notification::getRecipientId,
                        Notification::getType, Notification::getTargetId, Notification::getTargetTitle,
                        Notification::getReasonCode, Notification::getReason,
                        Notification::getStatus, Notification::getIsRead)
                .containsExactly(null, 22L, NotificationConstants.TYPE_NOTE_DOWNED, 1001L, "测试笔记",
                        "LOW_QUALITY", "内容低质", NotificationConstants.STATUS_ACTIVE, false);
        assertThat(inserted.getValue().getCreateTime()).isEqualTo(occurredAt);
    }

    @Test
    void duplicateEventDoesNotCreateAnotherNotification() {
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(0);

        NotificationMutationResult result = service.noteDowned(
                "down-1", 22L, 1001L, "测试笔记", "LOW_QUALITY", "内容低质", LocalDateTime.now());

        assertThat(result.isDuplicate()).isTrue();
        verify(notificationMapper, never()).insert(any(Notification.class));
    }
}
