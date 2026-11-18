package wcg.yhm.note.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.note.api.domain.event.NoteModerationEvent;
import wcg.yhm.note.domain.dataobject.NoteNotificationOutbox;
import wcg.yhm.note.domain.mapper.NoteNotificationOutboxMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteNotificationOutboxPublisherTest {
    private NoteNotificationOutboxMapper mapper;
    private RocketMQTemplate rocketMQTemplate;
    private NoteNotificationOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(NoteNotificationOutbox.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), NoteNotificationOutbox.class);
        }
        mapper = mock(NoteNotificationOutboxMapper.class);
        rocketMQTemplate = mock(RocketMQTemplate.class);
        publisher = new NoteNotificationOutboxPublisher();
        ReflectionTestUtils.setField(publisher, "mapper", mapper);
        ReflectionTestUtils.setField(publisher, "rocketMQTemplate", rocketMQTemplate);
        ReflectionTestUtils.setField(publisher, "batchSize", 10);
        ReflectionTestUtils.setField(publisher, "maxRetrySeconds", 300);
    }

    @Test
    void publishesPendingEventToModerationTopic() {
        when(mapper.selectList(any())).thenReturn(List.of(event()));

        publisher.publishPending();

        verify(rocketMQTemplate).syncSend(
                org.mockito.ArgumentMatchers.eq(NoteModerationEvent.TOPIC + ":" + NoteModerationEvent.TAG_DOWNED),
                anyString());
        verify(mapper).update(isNull(), any());
    }

    @Test
    void recordsFailureForLaterRetry() {
        when(mapper.selectList(any())).thenReturn(List.of(event()));
        when(rocketMQTemplate.syncSend(anyString(), anyString()))
                .thenThrow(new RuntimeException("MQ unavailable"));

        publisher.publishPending();

        verify(mapper).update(isNull(), any());
    }

    private NoteNotificationOutbox event() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 15, 18, 0);
        return NoteNotificationOutbox.builder()
                .id(1L)
                .eventId("event-1")
                .eventType(NoteModerationEvent.EVENT_NOTE_DOWNED)
                .noteId(1001L)
                .recipientId(22L)
                .noteTitle("测试笔记")
                .reasonCode("LOW_QUALITY")
                .reason("内容低质")
                .occurredAt(now)
                .schemaVersion(NoteModerationEvent.SCHEMA_VERSION_V1)
                .status(0)
                .attempts(0)
                .nextRetryTime(now)
                .build();
    }
}
