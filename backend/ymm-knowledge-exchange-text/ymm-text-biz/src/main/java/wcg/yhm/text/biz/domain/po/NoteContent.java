package wcg.yhm.text.biz.domain.po;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: 内容实体类
 * @Version: 1.0
 */

@Data
@Table(value = "note_content") //指定表
public class NoteContent {
    /** 笔记正文的 UUID 主键 */
    @PrimaryKey("id")
    private UUID id;

    /** 笔记正文原文 */
    private String content;
}
    
