package wcg.yhm.user.relation.biz.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.user.relation.api.domain.request.QueryFollowRequest;
import wcg.yhm.user.relation.api.domain.response.FollowResultResponse;
import wcg.yhm.user.relation.biz.domain.dto.FindFollowUserRequest;
import wcg.yhm.user.relation.biz.domain.dto.FollowingRequest;
import wcg.yhm.user.relation.biz.domain.dto.UnFollowRequest;
import wcg.yhm.user.relation.biz.domain.po.Following;
import wcg.yhm.user.relation.biz.domain.vo.FindFollowUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;

import java.util.List;

/**
 * <p>
 * 用户关注表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-01-25
 */
public interface IFollowingService extends IService<Following> {

    /**
     * 关注用户
     * @param followingRequest 关注用户id
     */
    void followUser(FollowingRequest followingRequest);

    /**
     * 取关用户
     * @param unFollowRequest 取关
     */
    void unfollowUser(UnFollowRequest unFollowRequest);

    /**
     * 分页查询用户
     * @return
     */
    PageResponse<FindFollowUserResponse> findFollowingList(FindFollowUserRequest findFollowUserRequest);

    /**
     * 查询登陆用户是否关注集合中的作者
     * @param queryFollowRequest 请求
     * @return 结果
     */
    List<FollowResultResponse> checkFollow(QueryFollowRequest queryFollowRequest);
}
