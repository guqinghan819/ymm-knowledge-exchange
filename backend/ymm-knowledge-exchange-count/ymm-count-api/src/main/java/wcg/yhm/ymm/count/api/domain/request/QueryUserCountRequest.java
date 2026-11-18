package wcg.yhm.ymm.count.api.domain.request;

import jakarta.validation.constraints.NotNull;
import jdk.jfr.DataAmount;
import lombok.Data;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-14
 * @Description:
 * @Version: 1.0
 */

@Data
public class QueryUserCountRequest {

    @NotNull(message = "用户id不能为空")
    private Long userId;
}
    
