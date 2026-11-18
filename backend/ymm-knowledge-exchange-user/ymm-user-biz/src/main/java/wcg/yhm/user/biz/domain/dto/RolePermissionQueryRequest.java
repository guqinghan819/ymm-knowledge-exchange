package wcg.yhm.user.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class RolePermissionQueryRequest implements Serializable {

    @NotNull(message = "roleId不能为空")
    private Long roleId;
}
