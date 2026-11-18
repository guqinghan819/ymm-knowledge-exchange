package wcg.yhm.user.relation.biz.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.user.relation.biz.domain.po.Fans;
import wcg.yhm.user.relation.biz.domain.vo.FanDailyAddedResponse;
import wcg.yhm.user.relation.api.domain.response.FanRankResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 用户粉丝表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2026-01-25
 */
public interface FansMapper extends BaseMapper<Fans> {

    List<FanDailyAddedResponse> selectDailyAdded(@Param("userId") Long userId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    List<FanRankResponse> selectAdminFanRank(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("limit") int limit);
}
