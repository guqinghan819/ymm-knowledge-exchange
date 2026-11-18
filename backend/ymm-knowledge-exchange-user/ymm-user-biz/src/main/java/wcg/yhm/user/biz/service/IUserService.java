package wcg.yhm.user.biz.service;



import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import wcg.yhm.user.api.domain.dto.*;
import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.user.biz.domain.dto.DeleteRequest;
import wcg.yhm.user.biz.domain.dto.QueryUserDetail;
import wcg.yhm.user.biz.domain.dto.UserPageQueryRequest;
import wcg.yhm.user.biz.domain.dto.UserUpdateRequest;
import wcg.yhm.user.biz.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.user.biz.domain.vo.UserDetailResponse;
import wcg.yhm.user.biz.domain.vo.UserManageResponse;
import wcg.yhm.user.biz.domain.vo.UserResponse;

import java.util.List;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-28
 */
public interface IUserService extends IService<User> {

    /**
     * 更新用户信息
     * @param userUpdateRequest 编辑信息
     */
    void updateUser(UserUpdateRequest userUpdateRequest);

    /**
     * 用户自动注册
     * @param userRegisterRequest 注册请求
     * @return
     */
    Long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 手机号查询密码id
     * @param getUserByPhoneRequest
     * @return
     */
    UserByPhoneResponse getByPhone(GetUserByPhoneRequest getUserByPhoneRequest);

    /**
     * 更新密码
     * @param updatePasswordRequest
     */
    void updatePassword(UpdatePasswordRequest updatePasswordRequest);

    /**
     * 获取当前登陆用户信息
     * @param id
     * @return
     */
    UserResponse getLoginUser(Long id);

    /**
     * 分页查询所有用户
     * @param userPageQueryRequest
     * @return
     */
    Page<UserManageResponse> getAllUser(UserPageQueryRequest userPageQueryRequest);

    /**
     * 根据id查询创作者大致信息
     * @param queryCreatorByIdRequest
     * @return
     */
    CreatorResponse getCreatorById(QueryCreatorByIdRequest queryCreatorByIdRequest);

    /**
     * 批量查询用户信息
     * @param getUserListByIdsRequest 用户ids
     * @return 用户列表
     */
    List<FindUserResponse> getUserListByIds(GetUserListByIdsRequest getUserListByIdsRequest);

    /**
     * 逻辑删除用户
     * @param deleteRequest
     */
    void deleteUser(DeleteRequest deleteRequest);

    /**
     * 查询用户数据
     * @param queryUserDetail 平台id
     * @return
     */
    UserDetailResponse queryUserDetail(QueryUserDetail queryUserDetail);
}
