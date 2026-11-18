package wcg.yhm.user.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserRoleQueryRequest implements Serializable {

    @NotNull(message = "userId不能为空")
    private Long userId;
}
