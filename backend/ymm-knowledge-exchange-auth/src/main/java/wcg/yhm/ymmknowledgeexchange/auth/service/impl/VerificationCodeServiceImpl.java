package wcg.yhm.ymmknowledgeexchange.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import wcg.yhm.ymmknowledgeexchange.auth.api.sms.AliyunSmsHandle;
import wcg.yhm.ymmknowledgeexchange.auth.constant.RedisKeyConstant;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.verification.VerificationRequest;
import wcg.yhm.ymmknowledgeexchange.auth.enums.LoginTypeEnum;
import wcg.yhm.ymmknowledgeexchange.auth.enums.ResponseCodeEnum;
import wcg.yhm.ymmknowledgeexchange.auth.enums.VerificationPurposeEnum;
import wcg.yhm.ymmknowledgeexchange.auth.service.VerificationCodeService;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.util.concurrent.TimeUnit;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description:实现类
 * @Version: 1.0
 */

@Service
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private AliyunSmsHandle aliyunSmsHandle;
    @Override
    public void sendVerificationCode(VerificationRequest verificationRequest) {
        //1.取出手机号
        String phone = verificationRequest.getPhone();
        Integer verificationType = verificationRequest.getVerificationType();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.getEnumByValue(verificationType);
        if (loginTypeEnum == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }
        //判断短信类型，根据类型发送不同用途短信，登陆，重置密码，修改手机号等
        VerificationPurposeEnum verificationPurposeEnum = VerificationPurposeEnum.getByValue(verificationType);
        if (verificationPurposeEnum == null) {
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }
        //2.建立redis key
        String key = "";
        switch (verificationPurposeEnum) {
            case USER_LOGIN  -> key = RedisKeyConstant.VERIFICATION_CODE_LOGIN_KEY_PREFIX + phone;
            case UPDATE_PASSWORD -> key = RedisKeyConstant.VERIFICATION_CODE_UPDATE_KEY_PREFIX + phone;
        }
        //3.判断key是否存在
        Boolean result = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(result)) {
            //3.1 存在抛出异常结束
            throw new BizException(ResponseCodeEnum.REQUEST_TOO_FREQUENT);
        }
        //3.2 不存在 生成6位验证码
        String verificationCode = RandomUtil.randomNumbers(6);
        log.info("=====> 手机号：{}, 已生成验证码: [{}]", phone, verificationCode);
        String singName = System.getenv("ALIYUN_SMS_SIGN_NAME");
        String templateCode = switch (verificationPurposeEnum) {
            case USER_LOGIN -> "100001";
            case UPDATE_PHONE -> "100002";
            case UPDATE_PASSWORD -> "100003";
            default -> "100001";
        };

        threadPoolTaskExecutor.submit(() -> {
            //4.调用短信服务 线程池异步执行
            //过期时间为1分钟
            String templateParam = String.format("{\"code\":\"%s\",\"min\":\"1\"}", verificationCode);
            aliyunSmsHandle.sendMessage(singName, templateCode, templateParam, phone);
        });
        //5.存入redis，设置有效期1分钟
        redisTemplate.opsForValue().set(key, verificationCode, 1, TimeUnit.MINUTES);
    }
}
    
