package wcg.yhm.ymmknowledgeexchange.auth.service;

import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.verification.VerificationRequest;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: 短信验证码服务
 * @Version: 1.0
 */


public interface VerificationCodeService {

    /**
     * 发送短信
     * @param verificationRequest 手机号
     *
     */
    void sendVerificationCode(VerificationRequest verificationRequest);
}
    
