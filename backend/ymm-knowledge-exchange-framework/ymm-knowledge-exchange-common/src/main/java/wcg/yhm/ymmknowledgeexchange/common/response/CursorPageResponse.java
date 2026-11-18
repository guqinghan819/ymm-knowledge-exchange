package wcg.yhm.ymmknowledgeexchange.common.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 游标分页响应类
 * @Version: 1.0
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class CursorPageResponse<T> extends Response<List<T>> {
    private Long nextCursor; //下一个游标
    private boolean hasNext; //是否还有数据
    private int size; //当前页的数据量
    private Long total; //总条数（可选）

    public static <T> CursorPageResponse<T> success(List<T> data, Long nextCursor, boolean hasNext, int size) {
        CursorPageResponse<T> cursorPageResponse = new CursorPageResponse<>();
        cursorPageResponse.setData(data);
        cursorPageResponse.setHasNext(hasNext);
        cursorPageResponse.setNextCursor(nextCursor);
        cursorPageResponse.setSize(size);
        return cursorPageResponse;
    }

    public static <T> CursorPageResponse<T> success(List<T> data, Long nextCursor, boolean hasNext, int size, Long total) {
        CursorPageResponse<T> page = success(data, nextCursor, hasNext, size);
        page.setTotal(total);
        return page;
    }
}
    
