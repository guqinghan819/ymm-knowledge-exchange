package wcg.yhm.text.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: 查询响应
 * @Version: 1.0
 */

@Data
public class QueryByIdResponse implements Serializable {

    private UUID id;
    private String content;
}
    
