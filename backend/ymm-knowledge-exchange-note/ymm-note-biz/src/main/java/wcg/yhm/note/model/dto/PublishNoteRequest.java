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
 * @CreateTime: 2025-12-23
 * @Description: 发布笔记请求
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublishNoteRequest implements Serializable {

    @NotNull(message = "笔记类型不能为空")
    private Integer type;

    /**
     * 图片地址列表
     */
    private List<String> imageUrls;

    /**
     * 视频地址
     */
    private String videoUrl;

    /**
     * 标题
     */
    private String title;

    /**
     * 笔记内容(最多6000字，可为空)
     */
    @Size(max = 6000, message = "正文内容不能超过6000字")
    private String content;

    /**
     * 关联的话题id
     */
    private Long topicId;
}
    
