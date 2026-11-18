package wcg.yhm.user.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.ErrorResponse;
import org.springframework.web.multipart.MultipartFile;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.user.api.domain.dto.*;
import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.user.biz.constant.RedisKeyConstant;
import wcg.yhm.user.biz.constant.RoleConstant;
import wcg.yhm.user.biz.domain.dto.DeleteRequest;
import wcg.yhm.user.biz.domain.dto.QueryUserDetail;
import wcg.yhm.user.biz.domain.dto.UserPageQueryRequest;
import wcg.yhm.user.biz.domain.dto.UserUpdateRequest;
import wcg.yhm.user.biz.domain.po.User;
import wcg.yhm.user.biz.domain.po.UserRoleRel;
import wcg.yhm.user.biz.domain.vo.UserDetailResponse;
import wcg.yhm.user.biz.domain.vo.UserManageResponse;
import wcg.yhm.user.biz.domain.vo.UserResponse;
import wcg.yhm.user.biz.enums.DeletedEnum;
import wcg.yhm.user.biz.enums.ErrorCodeEnum;
import wcg.yhm.user.biz.enums.StatusEnum;
import wcg.yhm.user.biz.mapper.UserMapper;
import wcg.yhm.user.biz.rpc.CountRpcService;
import wcg.yhm.user.biz.rpc.DistributedIdGeneratorRpcService;
import wcg.yhm.user.biz.rpc.OssRpcService;
import wcg.yhm.user.biz.service.RbacCacheService;
import wcg.yhm.user.biz.service.IUserRoleRelService;
import wcg.yhm.user.biz.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.user.biz.utils.CaffeineUtil;
import wcg.yhm.ymm.count.api.domain.response.UserCountResponse;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;
import wcg.yhm.ymmknowledgeexchange.common.utils.ParamUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-28
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private IUserRoleRelService userRoleRelService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private RbacCacheService rbacCacheService;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private CountRpcService countRpcService;

    @Override
    public void updateUser(UserUpdateRequest userUpdateRequest) {
        User user = new User();
        //1. 校验参数
        if (userUpdateRequest == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        String nickname = userUpdateRequest.getNickname();
        if (nickname != null) {
            if (!ParamUtils.checkNickname(nickname)) {
                throw new BizException(ErrorCodeEnum.NICK_NAME_LENGTH_ERROR);
            }
        }
        MultipartFile avatar = userUpdateRequest.getAvatar();
        if (Objects.nonNull(avatar)) {
            String url = ossRpcService.uploadFile(avatar);
            log.info("==> 调用 oss 服务成功，上传头像，url：{}", avatar);
            if (url == null || StrUtil.isBlank(url)) {
                throw new BizException(ErrorCodeEnum.AVATAR_UPLOAD_FAIL);
            }
            user.setAvatar(url);
        }
        MultipartFile backgroundImg = userUpdateRequest.getBackgroundImg();
        if (Objects.nonNull(backgroundImg)) {
            String url = ossRpcService.uploadFile(backgroundImg);
            log.info("==> 调用 oss 服务成功，上传背景图，url：{}", backgroundImg);
            if (url == null || StrUtil.isBlank(url)) {
                throw new BizException(ErrorCodeEnum.BACKGROUND_IMAGE_UPLOAD_FAIL);
            }

            user.setBackgroundImg(url);
        }
        String introduce = userUpdateRequest.getIntroduce();
        if (introduce != null) {
            if (!ParamUtils.checkTextLength(introduce, 150)) {
                throw new BizException(ErrorCodeEnum.INTRODUCE_ERROR);
            }
        }
        //忽略这两个字段，图片调用其他服务
         BeanUtil.copyProperties(userUpdateRequest, user, "avatar", "backgroundImg");
        user.setUpdateTime(LocalDateTime.now());

        boolean result = updateById(user);
        if (!result) {
            throw new BizException(ErrorCodeEnum.OPERATION_ERROR);
        }
    }

    /** 注册用户并分配普通用户角色；登录入口可复用已有的验证码账号。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long userRegister(UserRegisterRequest userRegisterRequest) {
        String phone = userRegisterRequest.getPhone();
        String encodedPassword = userRegisterRequest.getEncodedPassword();
        //判断是否注册
        User result = lambdaQuery().eq(User::getPhone, phone).one();
        if (result != null) {
            if (StrUtil.isNotBlank(encodedPassword)) {
                throw new BizException(ErrorCodeEnum.USER_ALREADY_EXISTS);
            }
            Long existingUserId = result.getId();
            rbacCacheService.afterCommit(() -> rbacCacheService.refreshUserRoles(existingUserId));
            return existingUserId;
        }
        //未注册就进行注册
        //构造参数
        User user = new User();
        String generatorUserId = distributedIdGeneratorRpcService.getUserId();
        log.info("============== generatorUserId {} ===============", generatorUserId);
        user.setId(Long.valueOf(generatorUserId));
        //使用redis的自增长获取全局唯一id
        //Long ymmID = redisTemplate.opsForValue().increment(RedisKeyConstant.YMM_ID_GENERATOR_KEY);
        //使用分布式生成
        String ymmId = distributedIdGeneratorRpcService.getYmmId();
        user.setYmmId(ymmId);
        user.setNickname("雨绵绵(,,・ω・,,) " + ymmId);
        user.setPhone(phone);
        user.setPassword(encodedPassword);
        user.setStatus(StatusEnum.ENABLE.getValue());
        user.setIntroduce("此人很懒，什么都没留下哦 ლ(́◕◞౪◟◕‵ლ)");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDelete(DeletedEnum.NO.getValue());
        //插入
        save(user);
        Long userId = user.getId();

        //分配默认角色
        UserRoleRel userRoleRel = new UserRoleRel();
        userRoleRel.setUserId(userId);
        userRoleRel.setCreateTime(LocalDateTime.now());
        userRoleRel.setUpdateTime(LocalDateTime.now());
        userRoleRel.setRoleId(RoleConstant.COMMON_USER_ROLE_ID);
        userRoleRel.setIsDeleted(DeletedEnum.NO.getValue());
        userRoleRelService.save(userRoleRel);

        // 等用户和角色关系提交成功后再刷新权限缓存，避免缓存读到未提交数据。
        rbacCacheService.afterCommit(() -> rbacCacheService.refreshUserRoles(userId));
        return userId;
    }

    @Override
    public UserByPhoneResponse getByPhone(GetUserByPhoneRequest getUserByPhoneRequest) {
        String phone = getUserByPhoneRequest.getPhone();
        User user = lambdaQuery().eq(User::getPhone, phone).one();
        if (user == null) {
            return null;
        }
        UserByPhoneResponse userByPhoneResponse = new UserByPhoneResponse();
        userByPhoneResponse.setPassword(user.getPassword());
        userByPhoneResponse.setId(user.getId());
        return userByPhoneResponse;
    }

    @Override
    public void updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        String encodePassword = updatePasswordRequest.getEncodePassword();
        //获取当前用户id
        Long userId = UserContextHolder.getUserId();
        User user = new User();
        user.setId(userId);
        user.setPassword(encodePassword);
        user.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(user);
        if (!result) {
            throw new BizException(ErrorCodeEnum.OPERATION_ERROR);
        }
    }

    @Override
    public UserResponse getLoginUser(Long id) {
        if (id == null || id < 0) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        User user = getById(id);
        UserResponse userResponse = new UserResponse();
        BeanUtil.copyProperties(user, userResponse);
        log.info("============== userResponse {} ==============", userResponse);
        return userResponse;
    }

    @Override
    public Page<UserManageResponse> getAllUser(UserPageQueryRequest userPageQueryRequest) {
        if (userPageQueryRequest == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        int currentPage = userPageQueryRequest.getCurrentPage();
        int pageSize = userPageQueryRequest.getPageSize();
        LambdaQueryWrapper<User> queryWrapper = getQueryWrapper(userPageQueryRequest);
        Page<User> userPage = page(new Page<>(currentPage, pageSize), queryWrapper);
        List<User> userList = userPage.getRecords();
        List<UserManageResponse> userManageResponseList = userList.stream()
                .map(UserManageResponse::copyBean)
                .toList();
        Page<UserManageResponse> userManageResponsePage = new Page<>(currentPage, pageSize, userPage.getTotal());
        userManageResponsePage.setRecords(userManageResponseList);
        return userManageResponsePage;
    }

    @Override
    public CreatorResponse getCreatorById(QueryCreatorByIdRequest queryCreatorByIdRequest) {
        Long creatorId = queryCreatorByIdRequest.getCreatorId();
        if (ObjectUtil.isEmpty(creatorId)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }

        //查询本地缓存
        CreatorResponse userInfo = CaffeineUtil.getUserInfo(creatorId);
        if (Objects.nonNull(userInfo)) {
            log.info("==========> 本地缓存命中: {}", userInfo);
            return userInfo;
        }

        //创建redis缓存key
        String userInfoKey = RedisKeyConstant.USER_INFO_KEY_PREFIX + creatorId;
        //查询redis缓存
        String userStr = (String)redisTemplate.opsForValue().get(userInfoKey);
        if (StrUtil.isNotBlank(userStr)) {
            CreatorResponse creatorResponse = JsonUtil.parseObject(userStr, CreatorResponse.class);
            //说明redis缓存中存在, 并存入本地缓存
            threadPoolTaskExecutor.submit(() -> {
               CaffeineUtil.saveUserInfo(creatorId, creatorResponse);
            });
            return creatorResponse;
        }
        //当缓存中没有该用户时，查询数据库
        User user = getById(creatorId);
        if (user == null) {
            threadPoolTaskExecutor.execute(() -> {
                //当用户不存在是同样进行缓存空值并设置过期时间，防止缓存穿透
                //保底一分钟 + 随机秒数
                long expireSecond = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(userInfoKey, "null", expireSecond, TimeUnit.SECONDS);
            });
            throw new BizException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        //db中查询到用户信息时进行异步缓存
        CreatorResponse creatorResponse = BeanUtil.copyProperties(user, CreatorResponse.class);
        threadPoolTaskExecutor.submit(() -> {
            //保底一天 + 随机秒数, 将过期时间打散，防止缓存雪崩
            long expireSecond = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            redisTemplate.opsForValue().set(userInfoKey, JsonUtil.toJsonString(creatorResponse),
                    expireSecond, TimeUnit.SECONDS);
        });
        return creatorResponse;
    }

    @Override
    public List<FindUserResponse> getUserListByIds(GetUserListByIdsRequest getUserListByIdsRequest) {
        List<Long> ids = getUserListByIdsRequest.getIds();
        //首先从redis中批量获取
        //构建redisKey
        List<String> redisKeyList = ids.stream().map(id -> RedisKeyConstant.USER_INFO_KEY_PREFIX + id).toList();
        //redis中全部命中，直接返回
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeyList);
        if (CollUtil.isNotEmpty(redisValues)) {
            //缓存中可能缓存了空值，进行过滤
            redisValues = redisValues.stream().filter(Objects::nonNull).toList();
        }
        List<FindUserResponse> userInfoList = new ArrayList<>();
        if (CollUtil.isNotEmpty(redisValues)) {
            userInfoList = redisValues.stream()
                    .map(value -> JsonUtil.parseObject(String.valueOf(value), FindUserResponse.class))
                    .collect(Collectors.toList());
        }
        //是否全部命中
        if (userInfoList.size() == ids.size()) {
            //直接返回
            return userInfoList;
        }
        //redis命中一部分，db查询另一部分再同步redis
        List<Long> needQueryIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(userInfoList)) {
            //转成map
            Map<Long, FindUserResponse> userMap = userInfoList.stream()
                    .collect(Collectors.toMap(FindUserResponse::getId, u -> u));
            //筛选出需要查询db的id
            needQueryIds = ids.stream()
                    .filter(id -> Objects.isNull(userMap.get(id)))
                    .collect(Collectors.toList());
        } else {
            //redis没命中，全部走db
            needQueryIds = ids;
        }
        //批量查询
        List<User> dbUser = lambdaQuery().in(User::getId, needQueryIds).list();
        if (CollUtil.isNotEmpty(dbUser)) {
            //将db数据与redis命中的一部分进行组合
            List<FindUserResponse> list = dbUser.stream()
                    .map(user -> BeanUtil.copyProperties(user, FindUserResponse.class))
                    .toList();
            userInfoList.addAll(list);

            //异步线程同步
            threadPoolTaskExecutor.submit(() -> {
                //将list -> map
                Map<Long, FindUserResponse> map = list.stream()
                        .collect(Collectors.toMap(FindUserResponse::getId, u -> u));
                //管道同步
                redisTemplate.executePipelined(new SessionCallback<>() {
                    @Override
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        for (User u : dbUser) {
                            Long id = u.getId();
                            String redisKey = RedisKeyConstant.USER_INFO_KEY_PREFIX + id;
                            String value = JsonUtil.toJsonString(map.get(id));
                            long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                            operations.opsForValue().set(redisKey, value, ttl, TimeUnit.SECONDS);
                        }
                        return null;
                    }
                });
            });
        }
        return userInfoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(DeleteRequest deleteRequest) {
        String ymmId = deleteRequest.getYmmId();
        User user = lambdaQuery().select(User::getId)
                .eq(User::getYmmId, ymmId)
                .eq(User::getIsDelete, DeletedEnum.NO.getValue())
                .one();
        if (user == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        User update = new User();
        update.setId(user.getId());
        update.setIsDelete(DeletedEnum.YES.getValue());
        update.setUpdateTime(LocalDateTime.now());
        if (!updateById(update)) {
            throw new BizException(ErrorCodeEnum.OPERATION_ERROR);
        }
        rbacCacheService.afterCommit(() -> rbacCacheService.refreshUserRoles(user.getId()));
    }

    @Override
    public UserDetailResponse queryUserDetail(QueryUserDetail queryUserDetail) {
        String ymmId = queryUserDetail.getYmmId();
        User user = lambdaQuery().select(User::getYmmId, User::getNickname, User::getId,
                User::getAvatar, User::getBackgroundImg,
                User::getSex, User::getIntroduce).eq(User::getYmmId, ymmId).eq(User::getIsDelete, DeletedEnum.NO.getValue()).one();
        UserDetailResponse userDetailResponse = new UserDetailResponse();
        BeanUtil.copyProperties(user, userDetailResponse);
        //查询用户数据
        UserCountResponse userCount = countRpcService.getUserCount(user.getId());
        BeanUtil.copyProperties(userCount, userDetailResponse);
        //todo 可进行缓存处理
        return userDetailResponse;
    }

    /**
     * 获取查询条件
     * @param userPageQueryRequest
     * @return
     */
    private LambdaQueryWrapper<User> getQueryWrapper(UserPageQueryRequest userPageQueryRequest) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        Long id = userPageQueryRequest.getId();
        String ymmId = userPageQueryRequest.getYmmId();
        String phone = userPageQueryRequest.getPhone();
        String nickname = userPageQueryRequest.getNickname();
        Integer status = userPageQueryRequest.getStatus();
        Integer sex = userPageQueryRequest.getSex();
        String sortField = userPageQueryRequest.getSortField();
        String sortOrder = userPageQueryRequest.getSortOrder();
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), User::getId, id)
                .like(StrUtil.isNotBlank(ymmId), User::getYmmId, ymmId)
                .like(StrUtil.isNotBlank(phone), User::getPhone, phone)
                .like(StrUtil.isNotBlank(nickname), User::getNickname, nickname)
                .eq(ObjectUtil.isNotEmpty(sex), User::getSex, sex)
                .eq(ObjectUtil.isNotEmpty(status), User::getStatus, status)
                .eq(User::getIsDelete, DeletedEnum.NO.getValue());
        // 添加排序逻辑
        if (StrUtil.isNotBlank(sortField)) {
            boolean isAsc = !"desc".equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "id":
                    queryWrapper.orderBy(true, isAsc, User::getId);
                    break;
                case "createTime":
                    queryWrapper.orderBy(true, isAsc, User::getCreateTime);
                    break;
                case "updateTime":
                    queryWrapper.orderBy(true, isAsc, User::getUpdateTime);
                    break;
                case "nickname":
                    queryWrapper.orderBy(true, isAsc, User::getNickname);
                    break;
                default:
                    queryWrapper.orderByDesc(User::getCreateTime);
                    break;
            }
        } else {
            // 默认按创建时间倒序排列
            queryWrapper.orderByDesc(User::getCreateTime);
        }
        return queryWrapper;
    }
}
