package wcg.yhm.note.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminDownNoteRequest implements Serializable {
    @NotNull(message = "笔记id不能为空")
    private Long noteId;

    @NotBlank(message = "下架原因不能为空")
    private String reasonCode;

    @Size(max = 200, message = "补充说明最多200个字符")
    private String reasonDetail;
}
