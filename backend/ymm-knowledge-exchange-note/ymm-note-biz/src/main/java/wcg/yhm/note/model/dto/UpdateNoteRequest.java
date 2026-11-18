package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-23
 * @Description: 更新笔记请求
 * @Version: 1.0
 */

@AllArgsConstructor
@Data
@NoArgsConstructor
public class UpdateNoteRequest implements Serializable {

    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    @NotNull(message = "笔记类型不能为空")
    private Integer type;

    private List<String> imgUris;

    private String videoUri;

    private String title;

    /**
     * 笔记内容(最多6000字)
     */
    @Size(max = 6000, message = "正文内容不能超过6000字")
    private String content;

    private Long topicId;

    private Integer visible;

    private Boolean isTop;
}
    
