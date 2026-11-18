package wcg.yhm.note.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class MyNotesPageResponse {

    private List<MyNoteItemResponse> pinnedNotes;

    private List<MyNoteItemResponse> normalNotes;

    private Long nextCursor;

    private boolean hasNext;

    private Long total;
}
