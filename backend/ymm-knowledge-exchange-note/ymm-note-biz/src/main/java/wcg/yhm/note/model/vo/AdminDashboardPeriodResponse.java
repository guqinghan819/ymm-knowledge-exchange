package wcg.yhm.note.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理端单个统计周期的数据。
 */
@Data
public class AdminDashboardPeriodResponse {

    private Integer days;

    private String startDate;

    private String endDate;

    private List<AdminUserRankResponse> topFans;

    private List<AdminUserRankResponse> topPublishers;

    private List<AdminChannelWordCloudResponse> channelWordCloud;
}
