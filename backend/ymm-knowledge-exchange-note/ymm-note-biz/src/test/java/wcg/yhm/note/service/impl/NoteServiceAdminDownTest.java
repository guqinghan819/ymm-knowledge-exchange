package wcg.yhm.note.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import wcg.yhm.note.api.domain.event.NoteRagIndexMessage;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.enums.NoteStatusEnum;
import wcg.yhm.note.model.dto.AdminDownNoteRequest;
import wcg.yhm.note.service.NoteNotificationOutboxService;
import wcg.yhm.note.service.NoteRagOutboxService;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

class NoteServiceAdminDownTest {
    private NoteRagOutboxService ragOutboxService;
    private NoteNotificationOutboxService notificationOutboxService;
    private NoteServiceImpl service;
    private LambdaQueryChainWrapper<Note> query;
    private LambdaUpdateChainWrapper<Note> update;
    private RocketMQTemplate rocketMQTemplate;

    @BeforeEach
    void setUp() {
        ragOutboxService = mock(NoteRagOutboxService.class);
        notificationOutboxService = mock(NoteNotificationOutboxService.class);
        service = spy(new NoteServiceImpl());
        query = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        update = mock(LambdaUpdateChainWrapper.class, RETURNS_SELF);
        when(query.eq(any(), any())).thenReturn(query);
        when(update.set(any(), any())).thenReturn(update);
        when(update.eq(any(), any())).thenReturn(update);
        doReturn(query).when(service).lambdaQuery();
        doReturn(update).when(service).lambdaUpdate();
        ReflectionTestUtils.setField(service, "noteRagOutboxService", ragOutboxService);
        ReflectionTestUtils.setField(service, "noteNotificationOutboxService", notificationOutboxService);
        ReflectionTestUtils.setField(service, "redisTemplate", mock(RedisTemplate.class));
        rocketMQTemplate = mock(RocketMQTemplate.class);
        ReflectionTestUtils.setField(service, "rocketMQTemplate", rocketMQTemplate);
    }

    @Test
    void otherReasonRequiresDetailsBeforeUpdatingNote() {
        AdminDownNoteRequest request = request("OTHER", "   ");

        assertThatThrownBy(() -> service.adminDownNote(request)).isInstanceOf(BizException.class);

        verify(query, never()).one();
        verify(notificationOutboxService, never()).appendDowned(any(), any(), any(), any());
    }

    @Test
    void invalidReasonIsRejectedBeforeUpdatingNote() {
        AdminDownNoteRequest request = request("UNKNOWN", null);

        assertThatThrownBy(() -> service.adminDownNote(request)).isInstanceOf(BizException.class);

        verify(query, never()).one();
    }

    @Test
    void reasonDetailsLongerThanTwoHundredCharactersAreRejected() {
        AdminDownNoteRequest request = request("LOW_QUALITY", "a".repeat(201));

        assertThatThrownBy(() -> service.adminDownNote(request)).isInstanceOf(BizException.class);

        verify(query, never()).one();
    }

    @Test
    void successfulDownWritesRagAndNotificationOutboxes() {
        Note note = new Note().setId(1001L).setCreatorId(22L).setTitle("测试笔记")
                .setStatus(NoteStatusEnum.NORMAL.getValue());
        when(query.one()).thenReturn(note);
        when(update.update()).thenReturn(true);

        service.adminDownNote(request("MISLEADING_CONTENT", "数据来源无法核实"));

        verify(ragOutboxService).append(any(), org.mockito.ArgumentMatchers.eq(NoteRagIndexMessage.EVENT_DELETE), any(LocalDateTime.class));
        verify(notificationOutboxService).appendDowned(
                org.mockito.ArgumentMatchers.eq(note),
                org.mockito.ArgumentMatchers.eq("MISLEADING_CONTENT"),
                org.mockito.ArgumentMatchers.eq("虚假或误导信息：数据来源无法核实"),
                any(LocalDateTime.class));
    }

    @Test
    void failedConditionalUpdateDoesNotWriteOutboxes() {
        Note note = new Note().setId(1001L).setCreatorId(22L).setTitle("测试笔记")
                .setStatus(NoteStatusEnum.NORMAL.getValue());
        when(query.one()).thenReturn(note);
        when(update.update()).thenReturn(false);

        assertThatThrownBy(() -> service.adminDownNote(request("LOW_QUALITY", null)))
                .isInstanceOf(BizException.class);

        verify(ragOutboxService, never()).append(any(), any(), any());
        verify(notificationOutboxService, never()).appendDowned(any(), any(), any(), any());
    }

    @Test
    void cacheBroadcastFailureDoesNotFailSuccessfulDown() {
        Note note = new Note().setId(1001L).setCreatorId(22L).setTitle("测试笔记")
                .setStatus(NoteStatusEnum.NORMAL.getValue());
        when(query.one()).thenReturn(note);
        when(update.update()).thenReturn(true);
        doThrow(new RuntimeException("MQ unavailable"))
                .when(rocketMQTemplate).syncSend(any(String.class), any(Long.class));

        assertThatCode(() -> service.adminDownNote(request("LOW_QUALITY", null)))
                .doesNotThrowAnyException();

        verify(notificationOutboxService).appendDowned(
                org.mockito.ArgumentMatchers.eq(note),
                org.mockito.ArgumentMatchers.eq("LOW_QUALITY"),
                org.mockito.ArgumentMatchers.eq("内容低质或与平台主题无关"),
                any(LocalDateTime.class));
    }

    private AdminDownNoteRequest request(String reasonCode, String reasonDetail) {
        AdminDownNoteRequest request = new AdminDownNoteRequest();
        request.setNoteId(1001L);
        request.setReasonCode(reasonCode);
        request.setReasonDetail(reasonDetail);
        return request;
    }
}
