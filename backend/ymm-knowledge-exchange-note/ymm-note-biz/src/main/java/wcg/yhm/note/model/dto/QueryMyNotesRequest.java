package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QueryMyNotesRequest {

    // 不传查全部（不含已删除），0=审核中，1=通过，3=未通过
    private Integer status;

    private Long cursor;

    private int limit = 20;
}
