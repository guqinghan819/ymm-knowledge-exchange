package wcg.yhm.ymm.count.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.ymm.count.domain.po.UserCount;

import java.util.List;

/**
 * <p>
 * 用户计数表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface UserCountMapper extends BaseMapper<UserCount> {

    /**
     * 插入或更新粉丝
     * @param userCounts 记录列表
     */
    void saveOrUpdateFansBatch(@Param("userCounts") List<UserCount> userCounts);

    /**
     * 插入或更新关注
     * @param userId
     * @param count
     */
    void saveOrUpdateFollowing(@Param("userId") Long userId,
                               @Param("count") Long count);

    /**
     * 插入或更新获赞
     * @param userCounts
     */
    void saveOrUpdateLikeTotalBatch(@Param("userCounts") List<UserCount> userCounts);

    /**
     * 插入或更新收藏
     * @param userCounts
     */
    void saveOrUpdateNoteCollectBatch(@Param("userCounts") List<UserCount> userCounts);

    /**
     * 插入或更新发布计数
     * @param userId
     * @param count
     */
    void saveOrUpdatePublishTotal(@Param("userId") Long userId, @Param("count") Long count);
}
