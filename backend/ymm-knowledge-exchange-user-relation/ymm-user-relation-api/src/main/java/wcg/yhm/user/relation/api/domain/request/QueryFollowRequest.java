package wcg.yhm.user.relation.api.domain.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 查询关注用户
 * @Version: 1.0
 */

@Data
public class QueryFollowRequest {

    @NotNull(message = "登陆用户id不能为空")
    private Long loginUserId; //登陆用户id

    @NotEmpty(message = "创作者id集合不能为空")
    private List<Long> creatorId; //创作者用户集合
}
    
