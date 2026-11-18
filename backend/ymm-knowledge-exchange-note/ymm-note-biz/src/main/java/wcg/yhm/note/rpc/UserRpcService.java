package wcg.yhm.note.rpc;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import wcg.yhm.user.api.controller.UserFeignApi;
import wcg.yhm.user.api.domain.dto.GetUserListByIdsRequest;
import wcg.yhm.user.api.domain.dto.QueryCreatorByIdRequest;
import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.note.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-22
 * @Description:
 * @Version: 1.0
 */

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 根据id查询创作者信息
     * @param creatorId 创作者id
     * @return
     */
    public CreatorResponse getCreator(Long creatorId) {
        QueryCreatorByIdRequest queryCreatorByIdRequest = new QueryCreatorByIdRequest();
        queryCreatorByIdRequest.setCreatorId(creatorId);
        Response<CreatorResponse> response = userFeignApi.getCreatorById(queryCreatorByIdRequest);
        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }

    /**
     * 批量查询用户信息
     * 用户服务批量查询接口单次限制最多10个id，这里按10个一组分批查询后合并
     * @param ids 用户id列表
     * @return 用户信息列表
     */
    public List<FindUserResponse> batchQueryUser(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<FindUserResponse> result = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += 10) {
            List<Long> chunk = ids.subList(i, Math.min(i + 10, ids.size()));
            GetUserListByIdsRequest request = new GetUserListByIdsRequest();
            request.setIds(chunk);
            Response<List<FindUserResponse>> response = userFeignApi.batchQueryUser(request);
            if (Objects.nonNull(response) && response.isSuccess() && CollUtil.isNotEmpty(response.getData())) {
                result.addAll(response.getData());
            }
        }
        return result;
    }

    /**
     * 管理端聚合场景使用的严格批量查询，远程服务异常时直接中止统计。
     */
    public List<FindUserResponse> batchQueryUserRequired(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        List<FindUserResponse> result = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += 10) {
            List<Long> chunk = ids.subList(i, Math.min(i + 10, ids.size()));
            GetUserListByIdsRequest request = new GetUserListByIdsRequest();
            request.setIds(chunk);
            Response<List<FindUserResponse>> response = userFeignApi.batchQueryUser(request);
            if (response == null || !response.isSuccess()) {
                throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
            }
            if (CollUtil.isNotEmpty(response.getData())) {
                result.addAll(response.getData());
            }
        }
        return result;
    }
}
    
