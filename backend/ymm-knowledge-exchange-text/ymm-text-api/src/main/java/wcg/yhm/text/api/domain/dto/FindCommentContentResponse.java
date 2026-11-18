package wcg.yhm.text.api.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description: 批量增加评论内容请求
 * @Version: 1.0
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindCommentContentResponse {

    /**
     * 评论内容 UUID
     */
    private String contentId;

    /**
     * 评论内容
     */
    private String content;

}