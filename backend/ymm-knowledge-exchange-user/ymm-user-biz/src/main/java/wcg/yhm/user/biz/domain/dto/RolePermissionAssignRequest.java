package wcg.yhm.user.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RolePermissionAssignRequest implements Serializable {

    @NotNull(message = "roleId不能为空")
    private Long roleId;

    @NotNull(message = "permissionIds不能为空")
    private List<Long> permissionIds;
}
