package wcg.yhm.ymm.count.api.domain.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 批量查询笔记计数请求
 * @Version: 1.0
 */

@Data
public class BatchQueryNoteCountRequest {
    @NotEmpty(message = "笔记id列表不能为空")
    private List<Long> noteIds;
}
    
