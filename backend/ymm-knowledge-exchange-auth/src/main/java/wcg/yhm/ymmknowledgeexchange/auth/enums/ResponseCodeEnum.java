package wcg.yhm.ymmknowledgeexchange.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.verification.VerificationRequest;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-20
 * @Description: 异常响应枚举
 * @Version: 1.0
 */

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseException {

    SYSTEM_ERROR("AUTH-10000", "系统异常, 牢勉正在紧急修复中, Ciallo～(∠・ω< )⌒★"),
    PARAM_NOT_VALID("AUTH-10001", "参数错误, Ciallo～(∠・ω< )⌒★"),
    REQUEST_TOO_FREQUENT("AUTH-20000", "请求太过频繁, 请稍后再试, ʅ（´◔౪◔）ʃ"),
    VERIFICATION_CODE_ERROR("AUTH-20001", "验证码错误, (((ﾟДﾟ;)))"),
    LOGIN_TYPE_ERROR("AUTH-20002", "登录类型错误 Σヽ(ﾟД ﾟ; )ﾉ"),
    USER_NOT_FOUND("AUTH-20003", "该用户不存在 Σヽ(ﾟД ﾟ; )ﾉ"),
    PHONE_OR_PASSWORD_ERROR("AUTH-20004", "手机号或密码错误 Σヽ(ﾟД ﾟ; )ﾉ"),
    CODE_PURPOSE_TYPE_ERROR("AUTH-20005", "短信用途类型错误 Σヽ(ﾟД ﾟ; )ﾉ"),
    UPDATE_PASSWORD_FAIL("AUTH-20007", "密码更新失败 Σヽ(ﾟД ﾟ; )ﾉ"),
    LOGIN_ERROR("AUTH-20006", "登陆失败 Σヽ(ﾟД ﾟ; )ﾉ");
    private final String errorCode;
    private final String errorMessage;

}
    
