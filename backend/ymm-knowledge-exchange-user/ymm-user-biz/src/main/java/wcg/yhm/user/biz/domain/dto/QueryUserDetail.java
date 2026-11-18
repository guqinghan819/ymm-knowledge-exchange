package wcg.yhm.user.biz.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-14
 * @Description: 查询用户信息详情请求
 * @Version: 1.0
 */

@Data
public class QueryUserDetail {
    @NotBlank(message = "平台id不能为空")
    private String ymmId;
}
    
