package wcg.yhm.user.biz.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import wcg.yhm.ymmknowledgeexchange.common.request.QueryPageRequest;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-09
 * @Description: 用户分页条件查询请求
 * @Version: 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserPageQueryRequest extends QueryPageRequest implements Serializable{

    private Long id;
    private String ymmId;
    private String nickname;
    private Integer sex;
    private String phone;
    private Integer status;

}
    
