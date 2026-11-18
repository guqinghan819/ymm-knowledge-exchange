package wcg.yhm.note.api.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 根据频道查询话题请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryTopicRequest {

    private Long id;
}
