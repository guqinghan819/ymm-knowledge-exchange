package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @Description: 管理端笔记操作请求(审核驳回/下架/恢复/强制删除)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminNoteOperateRequest implements Serializable {

    @NotNull(message = "笔记id不能为空")
    private Long noteId;
    private String reason;
}
