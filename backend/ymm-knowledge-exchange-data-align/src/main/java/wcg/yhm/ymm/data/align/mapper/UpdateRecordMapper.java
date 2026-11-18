package wcg.yhm.ymm.data.align.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-18
 * @Description: 更新
 * @Version: 1.0
 */

public interface UpdateRecordMapper {

    /**
     * 更新用户关注计数
     * @param userId 用户id
     * @param count 对齐数据
     */
    int updateUserFollowingCountByUserId(@Param("userId") Long userId, @Param("count") long count);
}
    
