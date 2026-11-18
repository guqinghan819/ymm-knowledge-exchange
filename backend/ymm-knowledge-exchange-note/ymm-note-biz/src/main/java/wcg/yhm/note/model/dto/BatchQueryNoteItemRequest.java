package wcg.yhm.note.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量查询笔记卡片请求。
 */
@Data
public class BatchQueryNoteItemRequest implements Serializable {

    private List<Long> noteIds;
}
