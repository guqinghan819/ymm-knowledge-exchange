package wcg.yhm.user.relation.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-02
 * @Description: 取关请求
 * @Version: 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnFollowRequest {
    /**
     * 取关用户id
     */
    @NotNull(message = "被取关用户id不可为空")
    private Long unfollowUserId;
}
    
