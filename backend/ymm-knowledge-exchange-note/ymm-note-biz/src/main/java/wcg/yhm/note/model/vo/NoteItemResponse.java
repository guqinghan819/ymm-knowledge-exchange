package wcg.yhm.note.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wcg.yhm.ymm.count.api.domain.response.NoteCountResponse;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 笔记列表详情
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoteItemResponse {

    private FindNoteDetailResponse findNoteDetailResponse; //笔记内容

    private NoteCountResponse noteCountResponse; //笔记计数

    private boolean isFollowing = false; //是否已经关注

    private boolean isLike = false; //点赞

    private boolean isCollect = false; //收藏

}
    
