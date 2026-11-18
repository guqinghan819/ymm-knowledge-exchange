package wcg.yhm.ymm.search.biz.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import wcg.yhm.ymm.search.biz.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.Optional;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-28
 * @Description: 全局异常处理器
 * @Version: 1.0
 */

@RestControllerAdvice //开启环绕切面 返回json，下面方法不用添加@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常捕捉
     * @param request 请求
     * @param bizException 业务异常
     * @return
     */
    @ExceptionHandler(BizException.class)
    public Response<Object> handleBizException(HttpServletRequest request, BizException bizException) {
        log.warn("{}: request fail, errorCode: {}, errorMessage: {}",
                request.getRequestURI(), bizException.getErrorCode(), bizException.getErrorMessage());
        return Response.fail(bizException);
    }

    /**
     * 捕获其他异常
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error, {}", request.getRequestURI(), e.getMessage());
        return Response.fail(ErrorCodeEnum.SYSTEM_ERROR);
    }


    /**
     * 捕获参数校验异常
     * @return
     */
    @ExceptionHandler({ MethodArgumentNotValidException.class })
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {
        // 参数错误异常码
        String errorCode = ErrorCodeEnum.PARAM_NOT_VALID.getErrorCode();
        // 获取 BindingResult
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder sb = new StringBuilder();
        // 获取校验不通过的字段，并组合错误信息，格式为： email 邮箱格式不正确, 当前值: '123124qq.com';
        Optional.ofNullable(bindingResult.getFieldErrors()).ifPresent(errors -> {
            errors.forEach(error ->
                    sb.append(error.getField())
                            .append(" ")
                            .append(error.getDefaultMessage())
                            .append(", 当前值: '")
                            .append(error.getRejectedValue())
                            .append("'; ")
            );
        });
        // 错误信息
        String errorMessage = sb.toString();
        log.warn("{} request error, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);
        return Response.fail(errorCode, errorMessage);
    }

}
    
