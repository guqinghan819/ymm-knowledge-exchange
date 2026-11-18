package wcg.yhm.note.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class NoteAnalysisPeriodResponse {

    private Integer days;

    private String startDate;

    private String endDate;

    private List<NoteAnalysisNoteItemResponse> likeTopNotes;

    private List<NoteAnalysisNoteItemResponse> collectTopNotes;

    private List<NoteAnalysisNoteItemResponse> commentTopNotes;

    private List<ChannelWordCloudItemResponse> channelWordCloud;
}
