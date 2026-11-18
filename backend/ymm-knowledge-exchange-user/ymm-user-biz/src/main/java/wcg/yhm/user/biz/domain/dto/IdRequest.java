package wcg.yhm.user.biz.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @Description: 按id操作请求(删除角色)
 */
@Data
public class IdRequest implements Serializable {

    @NotNull(message = "id不能为空")
    private Long id;
}
