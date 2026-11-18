package wcg.yhm.text.biz.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import wcg.yhm.text.biz.domain.po.CommentContent;
import wcg.yhm.text.biz.domain.po.CommentContentPrimaryKey;

import java.util.List;
import java.util.UUID;



/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: 操作类
 * @Version: 1.0
 *
 *  TextRepository<CommentContent, UUID> 是一个泛型接口 前面是实体类， 后面是主键类型
 */
public interface CommentContentRepository extends CassandraRepository<CommentContent, CommentContentPrimaryKey> {

    /**
     * 批量查询评论内容
     * @param noteId
     * @param yearMonths
     * @param contentIds
     * @return
     */
    List<CommentContent> findByPrimaryKeyNoteIdAndPrimaryKeyYearMonthInAndPrimaryKeyContentIdIn(
            Long noteId, List<String> yearMonths, List<UUID> contentIds
    );
}