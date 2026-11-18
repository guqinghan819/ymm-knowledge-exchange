package wcg.yhm.note.model.vo;

import lombok.Data;

/**
 * 管理端用户指标排名项。
 */
@Data
public class AdminUserRankResponse {

    private Long userId;

    private String nickname;

    private String avatar;

    private Long count = 0L;
}
