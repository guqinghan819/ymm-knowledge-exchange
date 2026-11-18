package wcg.yhm.user.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserRoleAssignRequest implements Serializable {

    @NotNull(message = "userId不能为空")
    private Long userId;

    @NotNull(message = "roleIds不能为空")
    private List<Long> roleIds;
}
