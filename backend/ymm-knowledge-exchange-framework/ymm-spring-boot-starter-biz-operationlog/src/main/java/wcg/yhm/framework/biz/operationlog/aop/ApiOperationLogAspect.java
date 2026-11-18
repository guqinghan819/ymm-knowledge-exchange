package wcg.yhm.framework.biz.operationlog.aop;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-20
 * @Description: 接口日志切面类
 * @Version: 1.0
 */

@Aspect
@Slf4j
public class ApiOperationLogAspect {


    /**
     * 定义切点(被ApiOperationLog注解标识)
     */
    @Pointcut("@annotation(wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog)")
    public void cutPoint() {}

    @Around("cutPoint()")
    /**
     *  环绕必须调用 joinPoint.proceed()
     *  Object 表示返回调用结果
     */
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        //请求开始时间
        long startTime = System.currentTimeMillis();
        //joinPoint.getTarget() 获取当前被代理的对象
        //获取类名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        //joinPoint.getSignature().getName() 获取方法名
        String methodName = joinPoint.getSignature().getName();
        //获取方法参数
        Object[] args = joinPoint.getArgs();
        String argStr = Arrays.stream(args)
                .map(JsonUtil::toJsonString)
                .collect(Collectors.joining(","));

        //获取功能描述
        //获取方法对象
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        ApiOperationLog annotation = method.getAnnotation(ApiOperationLog.class);
        String description = annotation.description();
        log.info("===========请求开始：[{}], 参数：[{}], 请求类名：[{}], 请求方法名：[{}]===================",
                description, argStr, className, methodName);
        //执行拦截方法并返回结果
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        log.info("===========请求结束：[{}], 耗时：[{} ms], 结果：[{}]===================",
                description, endTime - startTime, JsonUtil.toJsonString(result));
        return  result;
    }
}
    
