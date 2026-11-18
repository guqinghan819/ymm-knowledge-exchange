package wcg.yhm.ymmknowledgeexchange.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 手机号格式验证注解
 * @Version: 1.0
 */

@Documented
@Retention(RetentionPolicy.RUNTIME) //保留政策，运行时可通过反射访问
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE_PARAMETER, ElementType.ANNOTATION_TYPE}) //作用域 方法、方法参数、注解、字段
@Constraint(validatedBy = PhoneNumberValidator.class) //指定验证器
public @interface PhoneValidator {
    String message() default "手机号格式不正确 (*´･д･)?";
    //这两个属性是 JSR-380 Bean Validation 规范要求的必需属性，即使不使用分组和负载功能也需要声明。
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
    
