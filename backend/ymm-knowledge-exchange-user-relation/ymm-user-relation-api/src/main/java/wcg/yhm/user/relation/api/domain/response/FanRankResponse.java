package wcg.yhm.user.relation.api.domain.response;

import lombok.Data;

/**
 * 用户粉丝增长排名项。
 */
@Data
public class FanRankResponse {

    private Long userId;

    private Long count;
}
