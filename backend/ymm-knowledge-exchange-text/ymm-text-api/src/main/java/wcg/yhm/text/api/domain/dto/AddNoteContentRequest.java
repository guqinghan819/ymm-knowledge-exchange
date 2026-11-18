package wcg.yhm.text.api.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: 新增笔记内容请求
 * @Version: 1.0
 */

@Data
public class AddNoteContentRequest implements Serializable {
    @NotBlank(message = "id不能为空哦")
    private String id;
    @NotBlank(message = "内容不能为空哦")
    @Size(max = 6000, message = "正文内容不能超过6000字")
    private String content;
}
    
