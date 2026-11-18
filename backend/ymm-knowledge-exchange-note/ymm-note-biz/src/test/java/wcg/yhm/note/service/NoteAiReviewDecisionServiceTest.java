package wcg.yhm.note.service;

import org.junit.jupiter.api.Test;
import wcg.yhm.ai.api.ModerationResult;
import wcg.yhm.note.domain.dataobject.Note;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NoteAiReviewDecisionServiceTest {
    private ModerationResult result(boolean imagePass, String verdict, String reason) {
        ModerationResult result = new ModerationResult();
        result.setImagePass(imagePass);
        result.setTextVerdict(verdict);
        result.setTextReason(reason);
        return result;
    }

    @Test
    void imageFailureAlwaysUsesUniformAuthorReason() {
        assertEquals("图片不合规", NoteAiReviewDecisionService.classify(result(false, "PASS", "")).reason());
        assertEquals(Boolean.FALSE, NoteAiReviewDecisionService.classify(result(false, "REJECT", "辱骂内容")).approved());
    }

    @Test
    void textOutcomesAreConstrained() {
        assertEquals(Boolean.TRUE, NoteAiReviewDecisionService.classify(result(true, "PASS", "")).approved());
        assertEquals("含有人身攻击", NoteAiReviewDecisionService.classify(result(true, "REJECT", "含有人身攻击")).reason());
        assertNull(NoteAiReviewDecisionService.classify(result(true, "REVIEW", "")).approved());
        assertNull(NoteAiReviewDecisionService.classify(result(true, "PASS", "额外输出")).approved());
        assertNull(NoteAiReviewDecisionService.classify(result(true, "REJECT", "")).approved());
    }

    @Test
    void oldRevisionAndNonPendingNoteCannotBeReviewed() {
        Note note = new Note().setReviewVersion(2).setStatus(0).setType(0);
        assertTrue(NoteAiReviewTaskService.isCurrent(note, 2));
        assertFalse(NoteAiReviewTaskService.isCurrent(note, 1));
        note.setStatus(1);
        assertFalse(NoteAiReviewTaskService.isCurrent(note, 2));
    }

    @Test
    void duplicateCompletionCannotChangeNote() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        NoteAiReviewDecisionService service = new NoteAiReviewDecisionService();
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        service.finish(1L, 9L, 3, result(true, "PASS", ""));
        verify(jdbc, times(1)).update(anyString(), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void staleOrManualDecisionFailsVersionAndStatusGuard() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        NoteAiReviewDecisionService service = new NoteAiReviewDecisionService();
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        when(jdbc.update(startsWith("UPDATE note_ai_review_task SET text_result"), any(), any(), any(), any(), any())).thenReturn(1);
        service.finish(1L, 9L, 3, result(true, "PASS", ""));
        verify(jdbc).update(contains("status=0 AND type=0 AND review_version=? AND review_method IS NULL"),
                eq(1), isNull(), eq(9L), eq(3));
        verify(jdbc).update(contains("status='STALE'"), eq(1L));
    }
}
