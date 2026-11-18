package wcg.yhm.oss.biz.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import wcg.yhm.oss.biz.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-27
 * @Description: 全局异常处理器
 * @Version: 1.0
 */

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(BizException.class)
    public Response<Object> handleBizException(HttpServletRequest request, BizException e) {
        log.warn("{}: request fail, errorCode: {}, errorMessage: {}",
                request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        return Response.fail(e);
    }

    @ExceptionHandler(Exception.class)
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{}: request fail", request.getRequestURI(), e);
        return Response.fail(ErrorCodeEnum.OSS_SYSTEM_ERROR);
    }
}
    
