package wcg.yhm.note.model.dto;

import lombok.Data;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 游标分页获取笔记
 * @Version: 1.0
 */

@Data
public class SelectNoteRequest {

    private Long cursor;

    private String direction = "next"; //标识上一页(last)还是下一页(next)

    private int limit = 20;

    private Long channelId; //频道id

    private Long userId; //登陆用户id
}
    
