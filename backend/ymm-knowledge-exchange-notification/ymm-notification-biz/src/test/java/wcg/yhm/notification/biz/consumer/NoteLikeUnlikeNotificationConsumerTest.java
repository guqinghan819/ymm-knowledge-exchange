package wcg.yhm.notification.biz.consumer;

import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.note.api.domain.event.MqLikeUnlikeDTO;
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

class NoteLikeUnlikeNotificationConsumerTest {
    private NoteLikeUnlikeNotificationConsumer consumer;
    private NotificationEventService eventService;
    private NotificationFeedCacheService feedCacheService;

    @BeforeEach
    void setUp() {
        consumer = new NoteLikeUnlikeNotificationConsumer();
        eventService = mock(NotificationEventService.class);
        feedCacheService = mock(NotificationFeedCacheService.class);
        ReflectionTestUtils.setField(consumer, "eventService", eventService);
        ReflectionTestUtils.setField(consumer, "feedCacheService", feedCacheService);
    }

    @Test
    void selfLikeDoesNotCreateNotification() {
        MqLikeUnlikeDTO event = new MqLikeUnlikeDTO(
                "self-like", 1, 1001L, 22L, LocalDateTime.now(), 22L);

        consumer.onMessage(message("Like", event));

        verifyNoInteractions(eventService, feedCacheService);
    }

    @Test
    void likeMessageCreatesNotificationAndUpdatesFeed() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 15, 13, 0);
        MqLikeUnlikeDTO event = new MqLikeUnlikeDTO(
                "like-operation", 1, 1001L, 11L, occurredAt, 22L);
        NotificationMutationResult result = NotificationMutationResult.builder()
                .recipientId(22L)
                .addedNotificationId(303L)
                .removedNotificationIds(List.of())
                .build();
        when(eventService.noteLike("like-operation", 11L, 22L, 1001L, occurredAt))
                .thenReturn(result);

        consumer.onMessage(message("Like", event));

        verify(eventService).noteLike("like-operation", 11L, 22L, 1001L, occurredAt);
        verify(feedCacheService).applyMutation(result);
    }

    private Message message(String tag, MqLikeUnlikeDTO event) {
        return new Message("NoteLikeOrUnlike", tag,
                JsonUtil.toJsonString(event).getBytes(StandardCharsets.UTF_8));
    }
}
