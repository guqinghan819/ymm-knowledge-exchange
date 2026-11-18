package wcg.yhm.user.relation.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-03
 * @Description: 响应类
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindFansResponse {
    private Long userId;
    private String avatar;
    private String nickname;
    private Long noteTotal; //笔记总数
    private Long fansTotal; //粉丝数量
}
    
