package wcg.yhm.user.relation.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.user.relation.biz.domain.dto.FindFansRequest;
import wcg.yhm.user.relation.biz.domain.po.Fans;
import wcg.yhm.user.relation.biz.domain.vo.FanAnalysisResponse;
import wcg.yhm.user.relation.biz.domain.vo.FindFansResponse;
import wcg.yhm.user.relation.api.domain.request.AdminFanRankRequest;
import wcg.yhm.user.relation.api.domain.response.FanRankResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;

import java.util.List;

/**
 * <p>
 * 用户粉丝表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-01-25
 */
public interface IFansService extends IService<Fans> {

    /**
     * 查询粉丝列表
     * @param findFansRequest 目标用户id
     * @return 粉丝列表
     */
    PageResponse<FindFansResponse> findFansList(FindFansRequest findFansRequest);

    /**
     * 查询当前登录用户的粉丝新增分析。
     */
    FanAnalysisResponse analysis();

    /**
     * 查询指定时间范围内仍有效的新增粉丝排名。
     */
    List<FanRankResponse> adminFanRank(AdminFanRankRequest request);
}
