package wcg.yhm.ymm.search.biz.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description: 用户搜索请求
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchUserRequest {
    @NotBlank(message = "搜索关键字不能为空")
    private String keyword;

    @Min(value = 1, message = "页码不能小于1")
    private int pageNo = 1; //默认第一页

}
    
