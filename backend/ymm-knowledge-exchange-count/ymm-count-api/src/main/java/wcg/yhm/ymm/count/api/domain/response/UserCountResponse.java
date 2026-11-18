package wcg.yhm.ymm.count.api.domain.response;

import lombok.Data;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-14
 * @Description:
 * @Version: 1.0
 */

@Data
public class UserCountResponse {

    private Long userId;

    private Long followingTotal = 0L;

    private Long likeTotal = 0L;

    private Long noteTotal = 0L;

    private Long collectTotal = 0L;

    private Long fansTotal = 0L;
}
    
