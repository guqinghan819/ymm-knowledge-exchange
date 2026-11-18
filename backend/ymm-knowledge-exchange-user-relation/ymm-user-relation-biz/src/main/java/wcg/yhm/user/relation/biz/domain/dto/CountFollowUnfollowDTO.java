package wcg.yhm.user.relation.biz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-04
 * @Description:
 * @Version: 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CountFollowUnfollowDTO {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 目标用户id
     */
    private Long targetUserId;

    /**
     * 1: 关注 0: 取关
     */
    private Integer type;
}
    
