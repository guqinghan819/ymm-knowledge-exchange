package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-22
 * @Description: 获取笔记详情请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindNoteDetailRequest implements Serializable {

    @NotNull(message = "笔记id不能为空")
    private Long id;
}
    
