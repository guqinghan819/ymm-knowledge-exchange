package wcg.yhm.user.relation.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-02
 * @Description: 查询关注用户响应类
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindFollowUserResponse {
    private Long id;
    private String nickname;
    private String avatar;
    private String introduce;
}
    
