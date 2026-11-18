package wcg.yhm.ymmknowledgeexchange.common.response;

import lombok.Data;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-20
 * @Description: 通用响应类
 * @Version: 1.0
 */

@Data
public class Response<T> implements Serializable {
    private boolean success = true; //默认成功响应
    private String message; //响应消息
    private String errorCode; //异常码
    private T data; //响应数据

    public static <T> Response<T> success() {
        Response<T> response = new Response<>();
        return response;
    }

    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<>();
        response.setData(data);
        return response;
    }

    public static <T> Response<T> fail() {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        return response;
    }

    public static <T> Response<T> fail(String message) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public static <T> Response<T> fail(String message, String errorCode) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        return response;
    }

    public static <T> Response<T> fail(BaseException baseException) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(baseException.getErrorMessage());
        response.setErrorCode(baseException.getErrorCode());
        return response;
    }

    public static <T> Response<T> fail(BizException bizException) {
        Response<T> response = new Response<>();
        response.setSuccess(false);
        response.setMessage(bizException.getErrorMessage());
        response.setErrorCode(bizException.getErrorCode());
        return response;
    }
}
    
