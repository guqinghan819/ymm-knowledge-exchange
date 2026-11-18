package wcg.yhm.user.relation.biz.domain.vo;

import lombok.Data;

@Data
public class FanAnalysisResponse {

    private FanAnalysisPeriodResponse last7Days;

    private FanAnalysisPeriodResponse last30Days;
}
