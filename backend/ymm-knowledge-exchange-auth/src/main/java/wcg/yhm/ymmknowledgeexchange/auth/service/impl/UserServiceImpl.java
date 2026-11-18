package wcg.yhm.ymmknowledgeexchange.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.w3c.dom.stylesheets.LinkStyle;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.user.api.domain.dto.UpdatePasswordRequest;
import wcg.yhm.user.api.domain.dto.UserByPhoneResponse;
import wcg.yhm.ymmknowledgeexchange.auth.constant.RedisKeyConstant;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user.UserLoginRequest;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.user.UserUpdateRequest;

import wcg.yhm.ymmknowledgeexchange.auth.domain.po.User;

import wcg.yhm.ymmknowledgeexchange.auth.enums.LoginTypeEnum;
import wcg.yhm.ymmknowledgeexchange.auth.enums.ResponseCodeEnum;

import wcg.yhm.ymmknowledgeexchange.auth.mapper.UserMapper;
import wcg.yhm.ymmknowledgeexchange.auth.rpc.UserRpcService;
import wcg.yhm.ymmknowledgeexchange.auth.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-11-23
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserRpcService userRpcService;

    /** 验证码登录和密码登录均可自动注册；已注册账号的密码必须匹配才会建立登录态。 */
    @Override
    public String userLoginOrRegister(UserLoginRequest userLoginRequest) {
        //1.校验参数
        if (userLoginRequest == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }
        Integer loginType = userLoginRequest.getLoginType();
        String phone = userLoginRequest.getPhone();
        String code = userLoginRequest.getCode();
        String codeKey = RedisKeyConstant.VERIFICATION_CODE_LOGIN_KEY_PREFIX + phone;
        //2.判断登录方式
        LoginTypeEnum enumByValue = LoginTypeEnum.getEnumByValue(loginType);
        if (enumByValue == null) {
            throw new BizException(ResponseCodeEnum.LOGIN_TYPE_ERROR);
        }
        Long userId = null;
        switch(enumByValue) {
            case VERIFICATION_CODE:
                //从redis中取出验证码
                String rightCode = (String)redisTemplate.opsForValue().get(codeKey);
                //验证
                if (rightCode == null || !rightCode.equals(code)) {
                    throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }
                //远程调用用户服务 未注册就自动注册，已注册就返回
                userId = userRpcService.register(phone);
                if (userId == null) {
                    throw new BizException(ResponseCodeEnum.LOGIN_ERROR);
                }

                break;
            case PASSWORD:
                //取出密码
                String password = userLoginRequest.getPassword();
                if (StrUtil.isBlank(password)) {
                    throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
                }
                //进行加密后续用于比对
                password = encode(password);
                //远程调用手机号查询数据库
                UserByPhoneResponse byPhone = userRpcService.getByPhone(phone);
                if (byPhone == null) {
                    // 只在确认用户不存在时注册；注册接口会拒绝并发创建的同手机号账号。
                    userId = userRpcService.register(phone, password);
                    if (userId == null) {
                        throw new BizException(ResponseCodeEnum.LOGIN_ERROR);
                    }
                    break;
                }
                //比对密码
                if (!password.equals(byPhone.getPassword())) {
                    throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }
                userId = byPhone.getId();
                break;
            default:
                break;
        }
        //保存登录态
        StpUtil.login(userId);
        //获取token并返回
        return StpUtil.getTokenInfo().tokenValue;
    }

    @Override
    public void updatePassword(UserUpdateRequest userUpdateRequest) {
        //1.校验参数
        if (userUpdateRequest == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }
        //取出短信验证码
        String code = userUpdateRequest.getCode();
        String phone = userUpdateRequest.getPhone();
        String newPassword = userUpdateRequest.getNewPassword();
        if (StrUtil.isBlank(code) || StrUtil.isBlank(phone) || StrUtil.isBlank(newPassword)) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }
        String key = RedisKeyConstant.VERIFICATION_CODE_UPDATE_KEY_PREFIX + phone;
        //获取当前用户id
        Long userId = UserContextHolder.getUserId();
        //进行比对
        String rightCode = (String) redisTemplate.opsForValue().get(key);
        if (rightCode == null || !rightCode.equals(code)) {
            throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
        }
        //对新密码进行加密
        newPassword = encode(newPassword);
        //远程调用用户服务
        userRpcService.updatePassword(newPassword);
    }

    /**
     * 密码加密(娱乐性加密，严格可引入外部提供的加密方法)
     * @param newPassword 密码
     * @return
     */
    private String encode(String newPassword) {
        String salt = "yhm-king-is-so-handsome";
        return DigestUtils.md5DigestAsHex((salt + newPassword).getBytes());
    }

    /**
     * 自动注册用户
     */
//    private Long userRegister(String phone) {
//        //这里使用编程式事务，如果是注解式事务会发生自调用，事务会失效(可以将代理对象暴露，然后通过代理对象调用该方法)
//        return transactionTemplate.execute(status -> {
//            try {
//                //构造参数
//                User user = new User();
//                //使用redis的自增长获取全局唯一id
//                Long ymmID = redisTemplate.opsForValue().increment(RedisKeyConstant.YMM_ID_GENERATOR_KEY);
//                user.setYmmId(String.valueOf(String.valueOf(ymmID)));
//                user.setNickname("雨绵绵(,,・ω・,,) " + ymmID);
//                user.setPhone(phone);
//                user.setStatus(StatusEnum.ENABLE.getValue());
//                user.setIntroduce("此人很懒，什么都没留下哦 ლ(́◕◞౪◟◕‵ლ)");
//                user.setCreateTime(LocalDateTime.now());
//                user.setUpdateTime(LocalDateTime.now());
//                user.setIsDelete(DeletedEnum.NO.getValue());
//                //插入
//                save(user);
//                Long userId = user.getId();
//
//                //分配默认角色
//                UserRoleRel userRoleRel = new UserRoleRel();
//                userRoleRel.setUserId(userId);
//                userRoleRel.setCreateTime(LocalDateTime.now());
//                userRoleRel.setUpdateTime(LocalDateTime.now());
//                userRoleRel.setRoleId(RoleConstant.COMMON_USER_ROLE_ID);
//                userRoleRel.setIsDeleted(DeletedEnum.NO.getValue());
//                userRoleRelService.save(userRoleRel);
//
//                //将用户角色id存入redis中
//                List<String> roles = new ArrayList<>();
//                //使用用户id作为key
//                String roleKey = RedisKeyConstant.USER_ROLES_KEY_PREFIX + userId;
//                //根据角色id查询对应的角色键 common_user
//                Role role = roleService.getById(RoleConstant.COMMON_USER_ROLE_ID);
//                roles.add(role.getRoleKey());
//                redisTemplate.opsForValue().set(roleKey, JsonUtil.toJsonString(roles));
//                return userId;
//            } catch (Exception e) {
//                status.setRollbackOnly(); //标记事务回滚，调用后会将该事务标记为异常，后续不管抛没抛出异常都会回滚
//                log.error("=======> 注册服务异常, error {}", e.getMessage());
//                return null;
//            }
//        });
//    }


}
