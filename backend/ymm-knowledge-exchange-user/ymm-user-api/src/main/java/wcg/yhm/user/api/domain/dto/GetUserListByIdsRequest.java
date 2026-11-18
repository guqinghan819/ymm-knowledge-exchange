package wcg.yhm.user.api.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-02
 * @Description: 批量查询用户请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserListByIdsRequest {
    @NotNull(message = "用户id列表不能为空")
    @Size(min = 1, max = 10, message = "用户id集合大小仅能1-10")
    private List<Long> ids;
}
    
