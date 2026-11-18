package wcg.yhm.ymmknowledgeexchange.common.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-09
 * @Description: 通用分页查询类
 * @Version: 1.0
 */

@Data
public class QueryPageRequest implements Serializable {
    /**
     * 当前页号
     */
    private int currentPage = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;


    /**
     * 排序顺序(默认升序)
     */
    private String sortOrder = "descend";
}
    
