package wcg.yhm.framework.context.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.constant.GlobalConstant;

import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-28
 * @Description: feign拦截器 将userId 添加至请求头中
 * @Version: 1.0
 */

@Slf4j
public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        //获取当前用户id
        Long userId = UserContextHolder.getUserId();
        if (Objects.nonNull(userId)) {
            requestTemplate.header(GlobalConstant.USER_ID, String.valueOf(userId));
            log.info("============ FeignInterceptor userId: [{}]", userId);
        }
    }
}
    
