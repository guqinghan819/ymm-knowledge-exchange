package wcg.yhm.ymm.data.align.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-18
 * @Description: 删除已经对齐了的记录
 * @Version: 1.0
 */

public interface DeleteRecordMapper {

    /**
     * 日增量表：关注数计数变更 - 批量删除
     * @param userIds
     */
    void batchDeleteDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                     @Param("userIds") List<Long> userIds);
}
    
