package wcg.yhm.text.biz.domain.po;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-03
 * @Description: 评论内容实体类
 * @Version: 1.0
 */

@Data
@Table(value = "comment_content")
public class CommentContent {
    /** 评论正文的 Cassandra 复合主键 */
    @PrimaryKey
    private CommentContentPrimaryKey primaryKey;

    /** 评论正文原文 */
    private String content;
}
    
