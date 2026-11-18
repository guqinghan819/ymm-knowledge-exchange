package wcg.yhm.note.model.dto;

import lombok.Data;

/**
 * @Author: 雨好绵
 * @Description: 管理端笔记列表请求
 */
@Data
public class AdminNoteListRequest {

    /**
     * 笔记状态(0:待审核 1:正常 2:删除 3:下架/未通过)，不传查全部
     */
    private Integer status;

    /**
     * 创作者id，不传查全部
     */
    private Long creatorId;

    /**
     * 游标(上一页最后一条记录id)，不传查第一页
     */
    private Long cursor;

    /**
     * 每页数量
     */
    private int limit = 20;
}
