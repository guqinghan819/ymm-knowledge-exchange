package wcg.yhm.ymmknowledgeexchange.gateway.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;
import wcg.yhm.ymmknowledgeexchange.gateway.enums.ResponseCodeEnum;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-24
 * @Description: 全局异常处理器
 * @Version: 1.0
 */

@Slf4j
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 出现异常就会触发
     * @param exchange
     * @param ex
     * @return
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        //获取响应对象
        ServerHttpResponse response = exchange.getResponse();
        log.error("=======> 全局异常捕获(*´･д･)?: ", ex);
        Response<?> result;
        //对Sa-token出现的异常进行统一处理
        if (ex instanceof NotLoginException) {
            //未登录异常
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //考虑到有令牌过期和未携带令牌，直接用本身的消息
            result = Response.fail(ex.getMessage(), ResponseCodeEnum.UNAUTHORIZED.getErrorCode());
        } else if (ex instanceof NotPermissionException || ex instanceof NotRoleException) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            result = Response.fail(ResponseCodeEnum.FORBIDDEN.getErrorMessage(),
                    ResponseCodeEnum.FORBIDDEN.getErrorCode());
        } else {
            // 其他异常，则统一提示 “系统繁忙” 错误
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            result = Response.fail(ResponseCodeEnum.SYSTEM_ERROR.getErrorMessage(),
                    ResponseCodeEnum.SYSTEM_ERROR.getErrorCode());
        }
        // 设置响应头的内容类型为 application/json;charset=UTF-8，表示响应体为 JSON 格式
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        // 设置 body 响应体
        return response.writeWith(Mono.fromSupplier(() -> { // 使用 Mono.fromSupplier 创建响应体
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                // 使用 ObjectMapper 将 result 对象转换为 JSON 字节数组
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(result));
            } catch (Exception e) {
                // 如果转换过程中出现异常，则返回空字节数组
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}
    
