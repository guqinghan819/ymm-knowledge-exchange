package wcg.yhm.ymmknowledgeexchange.gateway.filter;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-24
 * @Description: 将用户id保存至请求头中，用于后续服务使用
 * @Version: 1.0
 */

@Component
@Slf4j
@Order(-90)
public class SaveUserId2HeaderFilter implements GlobalFilter {

    private static final String USER_ID = "userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 丢弃客户端传入的 userId，只信任从有效 token 解析出的身份。
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(USER_ID))
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();
        SaReactorSyncHolder.setContext(sanitizedExchange);
        //取出userId
        Long userId = null;
        try {
            //获取不到时报出cn.dev33.satoken.exception.NotLoginException
            userId = StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            //如果未登录(没有用户id)直接放行
            log.info(" ==================== e: {} =============", e.toString());
            return chain.filter(sanitizedExchange);
        }
        log.info("===========> 当前登录的用户id: {}", userId);

        //将userId放入请求头中
        ServerHttpRequest newRequest = sanitizedRequest.mutate().header(USER_ID, String.valueOf(userId)).build();
        //重新构建
        ServerWebExchange newExchange = sanitizedExchange.mutate().request(newRequest).build();
        return chain.filter(newExchange);
    }
}
    
