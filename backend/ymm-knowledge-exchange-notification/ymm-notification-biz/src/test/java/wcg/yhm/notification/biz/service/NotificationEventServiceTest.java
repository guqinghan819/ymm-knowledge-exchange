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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventServiceTest {
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
    void repeatedFollowEventDoesNotCreateOrRestoreNotification() {
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(0);

        NotificationMutationResult result = service.follow("same-event", 11L, 22L, LocalDateTime.now());

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getAddedNotificationId()).isNull();
        assertThat(result.getRemovedNotificationIds()).isEmpty();
        verify(notificationMapper, never()).selectList(any());
        verify(notificationMapper, never()).insert(any(Notification.class));
        verify(notificationMapper, never()).update(isNull(), any());
    }

    @Test
    void refollowRevokesOldNotificationAndCreatesANewUnreadOne() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 14, 12, 0);
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(1);
        when(notificationMapper.selectList(any())).thenReturn(List.of(new Notification().setId(101L)));
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(202L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        NotificationMutationResult result = service.follow("follow-2", 11L, 22L, occurredAt);

        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getRemovedNotificationIds()).containsExactly(101L);
        assertThat(result.getAddedNotificationId()).isEqualTo(202L);
        ArgumentCaptor<Notification> inserted = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(inserted.capture());
        assertThat(inserted.getValue())
                .extracting(Notification::getActorId, Notification::getRecipientId,
                        Notification::getType, Notification::getStatus, Notification::getIsRead)
                .containsExactly(11L, 22L, NotificationConstants.TYPE_FOLLOW,
                        NotificationConstants.STATUS_ACTIVE, false);
        assertThat(inserted.getValue().getCreateTime()).isEqualTo(occurredAt);
        verify(notificationMapper).update(isNull(), any());
    }

    @Test
    void unfollowRevokesActiveNotificationWithoutCreatingANegativeNotification() {
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(1);
        when(notificationMapper.selectList(any())).thenReturn(List.of(new Notification().setId(303L)));

        NotificationMutationResult result = service.unfollow("unfollow-1", 11L, 22L, LocalDateTime.now());

        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getRemovedNotificationIds()).containsExactly(303L);
        assertThat(result.getAddedNotificationId()).isNull();
        verify(notificationMapper, never()).insert(any(Notification.class));
        verify(notificationMapper).update(isNull(), any());
    }

    @Test
    void noteLikeCreatesUnreadNotificationWithNoteTarget() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 15, 12, 0);
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(1);
        when(notificationMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(404L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        NotificationMutationResult result = service.noteLike(
                "note-like-1", 11L, 22L, 9007199254740993L, occurredAt);

        assertThat(result.getAddedNotificationId()).isEqualTo(404L);
        assertThat(result.getRemovedNotificationIds()).isEmpty();
        ArgumentCaptor<Notification> inserted = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(inserted.capture());
        assertThat(inserted.getValue())
                .extracting(Notification::getActorId, Notification::getRecipientId,
                        Notification::getType, Notification::getTargetId,
                        Notification::getStatus, Notification::getIsRead)
                .containsExactly(11L, 22L, NotificationConstants.TYPE_NOTE_LIKE,
                        9007199254740993L, NotificationConstants.STATUS_ACTIVE, false);
        assertThat(inserted.getValue().getCreateTime()).isEqualTo(occurredAt);
    }

    @Test
    void noteUnlikeRevokesMatchingNotificationWithoutCreatingAnotherOne() {
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(1);
        when(notificationMapper.selectList(any())).thenReturn(List.of(new Notification().setId(505L)));

        NotificationMutationResult result = service.noteUnlike(
                "note-unlike-1", 11L, 22L, 1001L, LocalDateTime.now());

        assertThat(result.getAddedNotificationId()).isNull();
        assertThat(result.getRemovedNotificationIds()).containsExactly(505L);
        verify(notificationMapper, never()).insert(any(Notification.class));
        verify(notificationMapper).update(isNull(), any());
    }

    @Test
    void repeatedNoteLikeEventDoesNotCreateOrRestoreNotification() {
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(0);

        NotificationMutationResult result = service.noteLike(
                "same-note-like", 11L, 22L, 1001L, LocalDateTime.now());

        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getAddedNotificationId()).isNull();
        assertThat(result.getRemovedNotificationIds()).isEmpty();
        verify(notificationMapper, never()).selectList(any());
        verify(notificationMapper, never()).insert(any(Notification.class));
        verify(notificationMapper, never()).update(isNull(), any());
    }

    @Test
    void noteCollectCreatesUnreadNotificationWithNoteTarget() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 15, 15, 0);
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(1);
        when(notificationMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(606L);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        NotificationMutationResult result = service.noteCollect(
                "note-collect-1", 11L, 22L, 9007199254740993L, occurredAt);

        assertThat(result.getAddedNotificationId()).isEqualTo(606L);
        ArgumentCaptor<Notification> inserted = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(inserted.capture());
        assertThat(inserted.getValue())
                .extracting(Notification::getActorId, Notification::getRecipientId,
                        Notification::getType, Notification::getTargetId,
                        Notification::getStatus, Notification::getIsRead)
                .containsExactly(11L, 22L, NotificationConstants.TYPE_NOTE_COLLECT,
                        9007199254740993L, NotificationConstants.STATUS_ACTIVE, false);
    }

    @Test
    void noteUncollectRevokesMatchingNotificationWithoutCreatingAnotherOne() {
        when(eventLogMapper.insertIgnore(any(NotificationEventLog.class))).thenReturn(1);
        when(notificationMapper.selectList(any())).thenReturn(List.of(new Notification().setId(707L)));

        NotificationMutationResult result = service.noteUncollect(
                "note-uncollect-1", 11L, 22L, 1001L, LocalDateTime.now());

        assertThat(result.getAddedNotificationId()).isNull();
        assertThat(result.getRemovedNotificationIds()).containsExactly(707L);
        verify(notificationMapper, never()).insert(any(Notification.class));
        verify(notificationMapper).update(isNull(), any());
    }
}
