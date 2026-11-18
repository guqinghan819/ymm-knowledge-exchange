package wcg.yhm.ymm.data.align.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-18
 * @Description:
 * @Version: 1.0
 */

public interface SelectRecordMapper {

    /**
     * 日增量表：关注数计数变更 - 批量查询
     * @param tableNameSuffix
     * @param batchSize
     * @return
     */
    List<Long> selectBatchFromDataAlignFollowingCountTempTable(@Param("tableNameSuffix") String tableNameSuffix,
                                                               @Param("batchSize") int batchSize);

    /**
     * 查询 t_following 关注表，获取关注总数
     * @param userId
     * @return
     */
    long selectCountFromFollowingTableByUserId(long userId);
}
    
