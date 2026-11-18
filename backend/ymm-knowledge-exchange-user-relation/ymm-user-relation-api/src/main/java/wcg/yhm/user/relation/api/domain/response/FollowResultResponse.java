package wcg.yhm.user.relation.api.domain.response;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 关注结果响应类
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowResultResponse {

    private Long creatorId; //创作者id

    private boolean isFollow = false; // 是否关注(默认为false)
}
    
