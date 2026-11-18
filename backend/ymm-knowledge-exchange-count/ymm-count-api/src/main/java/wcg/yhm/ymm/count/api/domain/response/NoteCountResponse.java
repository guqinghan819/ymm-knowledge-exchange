package wcg.yhm.ymm.count.api.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 笔记计数响应
 * @Version: 1.0
 */

@Data
public class NoteCountResponse {

    private Long noteId;

    private Long likeTotal = 0L; //点赞数

    private Long collectTotal = 0L; //收藏数

    private Long commentTotal = 0L; //评论数
}
    
