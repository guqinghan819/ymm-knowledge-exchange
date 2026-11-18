package wcg.yhm.user.relation.api.domain.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端粉丝增长排名查询参数。
 */
@Data
public class AdminFanRankRequest {

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "排名数量不能为空")
    @Min(value = 1, message = "排名数量至少为1")
    @Max(value = 100, message = "排名数量最多为100")
    private Integer limit;
}
