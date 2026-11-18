package wcg.yhm.user.api.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-02
 * @Description: 批量查询响应类
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindUserResponse {

    private Long id;
    private String nickname;
    private String avatar;
    private String introduce;
}
    
