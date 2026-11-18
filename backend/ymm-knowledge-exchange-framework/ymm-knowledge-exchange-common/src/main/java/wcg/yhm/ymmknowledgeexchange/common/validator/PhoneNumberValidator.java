package wcg.yhm.ymmknowledgeexchange.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 手机号验证
 * @Version: 1.0
 *
 *
 * ConstraintValidator 是 Jakarta Bean Validation API 中的核心接口，用于实现自定义验证逻辑。
 * 主要特点：
 * 泛型接口：ConstraintValidator<A extends Annotation, T>，其中 A 是约束注解类型，T 是要验证的目标类型
 * 生命周期管理：提供 initialize() 和 isValid() 两个关键方法
 */

public class PhoneNumberValidator implements ConstraintValidator<PhoneValidator, String> {

    /**
     * 中国移动号段: 134, 135, 136, 137, 138, 139, 147, 150, 151, 152, 157, 158, 159, 178, 182, 183, 184, 187, 188, 198
     * 中国联通号段: 130, 131, 132, 145, 155, 156, 166, 171, 175, 176, 185, 186
     * 中国电信号段: 133, 149, 153, 173, 177, 180, 181, 189, 199
     */
    private static final String PHONE_PATTERN =
            "^((13[0-9])|(14[5|7|9])|(15([0-3]|[5-9]))|(16[6])|(17[0-3]|[5-8])|(18[0-9])|(19[8|9]))\\d{8}$";

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext constraintValidatorContext) {
        //手机格式校验
        return phone != null && phone.matches(PHONE_PATTERN);
    }

    @Override
    public void initialize(PhoneValidator constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
    
