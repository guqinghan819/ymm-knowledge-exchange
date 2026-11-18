package wcg.yhm.framework.context.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.constant.GlobalConstant;


import java.io.IOException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-25
 * @Description: 用户上下文过滤器
 * @Version: 1.0
 */

@Component
@Slf4j
public class SaveUserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //从请求头中获取userId 在全局过滤器中添加了头信息
        String userId = request.getHeader(GlobalConstant.USER_ID);
        if (StrUtil.isBlank(userId)) {
            //如果没有用户id直接放行(未登录)
            filterChain.doFilter(request, response);
            //提前退出：当 userId 为空时，直接放行请求并立即返回，不再执行后续代码
            return;
        }
        log.info("===========> saveContextFilter 获取到userId: [{}]", userId);
        //放入ThreadLocal中
        UserContextHolder.setUserId(userId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            //线程执行完后清楚信息
            UserContextHolder.removeThreadLocal();
            log.info("===========> saveContextFilter 删除用户信息");
        }
    }
}
    
