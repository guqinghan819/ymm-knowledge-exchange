package wcg.yhm.ymm.count.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.ymm.count.api.domain.request.QueryUserCountRequest;
import wcg.yhm.ymm.count.api.domain.response.UserCountResponse;
import wcg.yhm.ymm.count.domain.po.UserCount;

/**
 * <p>
 * 用户计数表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface IUserCountService extends IService<UserCount> {

    /**
     * 查询用户计数
     * @param queryUserCountRequest 用户id
     * @return
     */
    UserCountResponse getUserCount(QueryUserCountRequest queryUserCountRequest);
}
