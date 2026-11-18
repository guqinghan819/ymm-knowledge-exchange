package wcg.yhm.notification.biz.consumer;

import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.note.api.domain.event.NoteModerationEvent;
import wcg.yhm.notification.biz.domain.bo.NotificationMutationResult;
import wcg.yhm.notification.biz.service.NotificationEventService;
import wcg.yhm.notification.biz.service.NotificationFeedCacheService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoteDownedNotificationConsumerTest {
    private NoteDownedNotificationConsumer consumer;
    private NotificationEventService eventService;
    private NotificationFeedCacheService feedCacheService;

    @BeforeEach
    void setUp() {
        consumer = new NoteDownedNotificationConsumer();
        eventService = mock(NotificationEventService.class);
        feedCacheService = mock(NotificationFeedCacheService.class);
        ReflectionTestUtils.setField(consumer, "eventService", eventService);
        ReflectionTestUtils.setField(consumer, "feedCacheService", feedCacheService);
    }

    @Test
    void validEventCreatesNotificationAndUpdatesFeed() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 15, 18, 0);
        NoteModerationEvent event = event("event-1", "内容低质", occurredAt);
        NotificationMutationResult result = NotificationMutationResult.builder()
                .recipientId(22L)
                .addedNotificationId(99L)
                .removedNotificationIds(List.of())
                .build();
        when(eventService.noteDowned("event-1", 22L, 1001L, "测试笔记",
                "LOW_QUALITY", "内容低质", occurredAt)).thenReturn(result);

        consumer.onMessage(message(event));

        verify(eventService).noteDowned("event-1", 22L, 1001L, "测试笔记",
                "LOW_QUALITY", "内容低质", occurredAt);
        verify(feedCacheService).applyMutation(result);
    }

    @Test
    void eventWithoutReasonIsIgnored() {
        consumer.onMessage(message(event("event-2", "", LocalDateTime.now())));

        verifyNoInteractions(eventService, feedCacheService);
    }

    private NoteModerationEvent event(String eventId, String reason, LocalDateTime occurredAt) {
        return NoteModerationEvent.builder()
                .eventId(eventId)
                .eventType(NoteModerationEvent.EVENT_NOTE_DOWNED)
                .noteId(1001L)
                .recipientId(22L)
                .noteTitle("测试笔记")
                .reasonCode("LOW_QUALITY")
                .reason(reason)
                .occurredAt(occurredAt)
                .schemaVersion(NoteModerationEvent.SCHEMA_VERSION_V1)
                .build();
    }

    private Message message(NoteModerationEvent event) {
        return new Message(NoteModerationEvent.TOPIC, NoteModerationEvent.TAG_DOWNED,
                JsonUtil.toJsonString(event).getBytes(StandardCharsets.UTF_8));
    }
}
