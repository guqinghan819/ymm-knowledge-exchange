package wcg.yhm.framework.biz.operationlog.aop;

import java.lang.annotation.*;

/**
 * 接口日志注解
 */

@Documented
@Retention(RetentionPolicy.RUNTIME) //最高级别保留，运行时通过反射获取
@Target(ElementType.METHOD) //仅作用于方法
public @interface ApiOperationLog {

    /**
     * api功能描述
     */
    String description() default "";
}
