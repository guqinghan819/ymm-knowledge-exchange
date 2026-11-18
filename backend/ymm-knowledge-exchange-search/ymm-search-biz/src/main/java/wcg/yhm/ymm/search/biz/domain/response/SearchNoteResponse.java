package wcg.yhm.ymm.search.biz.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description: 用户搜索响应类
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchNoteResponse {

    private String cover; //封面

    private Long noteId; //笔记id

    private String title; //标题

    private String nickname; //昵称

    private String avatar; //头像

    private String likeTotal; //获赞

    private String highlightTitle; //标题: 关键词高亮

    private String updateTime;

    private String commentTotal;

    private String collectTotal;
}
    
