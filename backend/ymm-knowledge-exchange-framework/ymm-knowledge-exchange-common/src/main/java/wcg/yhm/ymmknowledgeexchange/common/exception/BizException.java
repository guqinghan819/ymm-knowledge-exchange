package wcg.yhm.ymmknowledgeexchange.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-20
 * @Description: 业务异常
 * @Version: 1.0
 */

@Getter
@Setter
public class BizException extends RuntimeException{
    private String errorCode;
    private String errorMessage;

    public BizException(BaseException baseException) {
        this.errorCode = baseException.getErrorCode();
        this.errorMessage = baseException.getErrorMessage();
    }
}
    
