package wcg.yhm.user.relation.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-02
 * @Description: 查询关注列表请求
 * @Version: 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindFollowUserRequest {

    @NotNull(message = "查询用户id不能为空")
    private Long userId;

    @NotNull(message = "页码不能为空")
    private Integer pageNo = 1; //默认为第一页
}
    
