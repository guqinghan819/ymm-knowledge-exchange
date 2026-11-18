package wcg.yhm.user.relation.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description: 关注请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowingRequest {
    @NotNull(message = "关注用户id不能为空")
    private Long followUserId;
}
    
