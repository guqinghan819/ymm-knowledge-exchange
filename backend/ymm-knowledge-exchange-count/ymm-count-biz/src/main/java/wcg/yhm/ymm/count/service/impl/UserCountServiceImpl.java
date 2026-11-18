package wcg.yhm.ymm.count.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.ymm.count.api.domain.request.QueryUserCountRequest;
import wcg.yhm.ymm.count.api.domain.response.UserCountResponse;
import wcg.yhm.ymm.count.domain.po.UserCount;
import wcg.yhm.ymm.count.mapper.UserCountMapper;
import wcg.yhm.ymm.count.service.IUserCountService;

/**
 * <p>
 * 用户计数表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
@Service
public class UserCountServiceImpl extends ServiceImpl<UserCountMapper, UserCount> implements IUserCountService {

    @Override
    public UserCountResponse getUserCount(QueryUserCountRequest queryUserCountRequest) {
        Long userId = queryUserCountRequest.getUserId();
        UserCount userCount = lambdaQuery().eq(UserCount::getUserId, userId).one();
        if (userCount == null) {
            UserCountResponse response = new UserCountResponse();
            response.setUserId(userId);
            return response;
        }
        UserCountResponse response = BeanUtil.copyProperties(userCount, UserCountResponse.class);
        response.setFollowingTotal(defaultZero(response.getFollowingTotal()));
        response.setLikeTotal(defaultZero(response.getLikeTotal()));
        response.setNoteTotal(defaultZero(response.getNoteTotal()));
        response.setCollectTotal(defaultZero(response.getCollectTotal()));
        response.setFansTotal(defaultZero(response.getFansTotal()));
        return response;
    }

    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }
}
