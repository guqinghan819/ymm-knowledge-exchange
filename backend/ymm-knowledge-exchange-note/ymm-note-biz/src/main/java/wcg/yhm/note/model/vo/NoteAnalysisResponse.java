package wcg.yhm.note.model.vo;

import lombok.Data;

@Data
public class NoteAnalysisResponse {

    private NoteAnalysisPeriodResponse last7Days;

    private NoteAnalysisPeriodResponse last30Days;
}
