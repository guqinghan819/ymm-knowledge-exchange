package wcg.yhm.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-11
 * @Description: 笔记收藏状态
 * @Version: 1.0
 */

@AllArgsConstructor
@Getter
public enum NoteCollectStatus {
    COLLECT(1), //收藏
    NOT_COLLECT(0), //没有收藏
    ;

    private final Integer code;
}
    
