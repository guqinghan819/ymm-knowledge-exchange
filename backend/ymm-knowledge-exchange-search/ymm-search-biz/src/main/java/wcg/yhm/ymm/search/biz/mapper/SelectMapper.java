package wcg.yhm.ymm.search.biz.mapper;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description:
 * @Version: 1.0
 */


public interface SelectMapper {

    /**
     * 查询笔记文档所需的全字段数据
     * @param noteId
     * @return
     */
    @MapKey("id")
    List<Map<String, Object>> selectEsNoteIndexData(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /**
     * 查询用户索引所需的全字段数据
     * @param userId
     * @return
     */
    @MapKey("id")
    List<Map<String, Object>> selectEsUserIndexData(@Param("userId") Long userId);
}

    
