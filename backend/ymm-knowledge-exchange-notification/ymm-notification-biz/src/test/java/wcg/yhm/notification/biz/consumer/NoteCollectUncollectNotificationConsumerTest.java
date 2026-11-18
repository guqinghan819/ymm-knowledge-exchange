package wcg.yhm.notification.biz.consumer;

import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.note.api.domain.event.MQCollectUnCollectDTO;
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

class NoteCollectUncollectNotificationConsumerTest {
    private NoteCollectUncollectNotificationConsumer consumer;
    private NotificationEventService eventService;
    private NotificationFeedCacheService feedCacheService;

    @BeforeEach
    void setUp() {
        consumer = new NoteCollectUncollectNotificationConsumer();
        eventService = mock(NotificationEventService.class);
        feedCacheService = mock(NotificationFeedCacheService.class);
        ReflectionTestUtils.setField(consumer, "eventService", eventService);
        ReflectionTestUtils.setField(consumer, "feedCacheService", feedCacheService);
    }

    @Test
    void selfCollectDoesNotCreateNotification() {
        MQCollectUnCollectDTO event = new MQCollectUnCollectDTO(
                "self-collect", 22L, 1001L, 1, LocalDateTime.now(), 22L);

        consumer.onMessage(message("Collect", event));

        verifyNoInteractions(eventService, feedCacheService);
    }

    @Test
    void collectMessageCreatesNotificationAndUpdatesFeed() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 15, 16, 0);
        MQCollectUnCollectDTO event = new MQCollectUnCollectDTO(
                "collect-operation", 11L, 1001L, 1, occurredAt, 22L);
        NotificationMutationResult result = NotificationMutationResult.builder()
                .recipientId(22L)
                .addedNotificationId(808L)
                .removedNotificationIds(List.of())
                .build();
        when(eventService.noteCollect("collect-operation", 11L, 22L, 1001L, occurredAt))
                .thenReturn(result);

        consumer.onMessage(message("Collect", event));

        verify(eventService).noteCollect("collect-operation", 11L, 22L, 1001L, occurredAt);
        verify(feedCacheService).applyMutation(result);
    }

    private Message message(String tag, MQCollectUnCollectDTO event) {
        return new Message("NoteCollectOrUnCollect", tag,
                JsonUtil.toJsonString(event).getBytes(StandardCharsets.UTF_8));
    }
}
