package wcg.yhm.user.relation.biz.domain.vo;

import lombok.Data;

@Data
public class FanDailyAddedResponse {

    private String date;

    private Long count = 0L;
}
