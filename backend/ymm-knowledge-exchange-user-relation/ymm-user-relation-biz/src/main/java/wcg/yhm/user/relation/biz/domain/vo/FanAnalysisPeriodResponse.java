package wcg.yhm.user.relation.biz.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class FanAnalysisPeriodResponse {

    private Integer days;

    private String startDate;

    private String endDate;

    private Long addedTotal = 0L;

    private List<FanDailyAddedResponse> dailyAdded;
}
