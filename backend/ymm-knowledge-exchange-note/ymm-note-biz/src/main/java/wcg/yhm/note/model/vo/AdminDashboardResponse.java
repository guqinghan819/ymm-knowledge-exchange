package wcg.yhm.note.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理端分析面板响应。
 */
@Data
public class AdminDashboardResponse {

    private AdminDashboardPeriodResponse last7Days;

    private AdminDashboardPeriodResponse last30Days;

    private List<AdminLikedNoteResponse> topLikedNotes;
}
