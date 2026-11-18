package wcg.yhm.ymm.search.biz.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description: 用户搜索响应类
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchUserResponse {

    private String avatar;

    private String ymm_id;

    private String nickname;

    private String fansTotal;

    private Long noteTotal;

    private Long userId;

    private String highlightNickname; //昵称: 关键词高亮
}
    
