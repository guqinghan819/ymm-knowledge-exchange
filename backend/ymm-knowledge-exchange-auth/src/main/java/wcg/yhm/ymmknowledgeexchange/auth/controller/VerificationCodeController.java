package wcg.yhm.ymmknowledgeexchange.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import wcg.yhm.framework.biz.operationlog.aop.ApiOperationLog;
import wcg.yhm.ymmknowledgeexchange.auth.domain.dto.verification.VerificationRequest;
import wcg.yhm.ymmknowledgeexchange.auth.service.VerificationCodeService;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: 短信接口
 * @Version: 1.0
 */

@RestController
@RequestMapping("/verification")
@Slf4j
@Tag(name = "短信接口")
public class VerificationCodeController {

    @Resource
    private VerificationCodeService verificationCodeService;

    @PostMapping("/code/send")
    @ApiOperationLog(description = "发送短信验证码")
    @Operation(summary = "发送短信")
    public Response<?> send(@Validated @RequestBody VerificationRequest verificationRequest) {
        verificationCodeService.sendVerificationCode(verificationRequest);
        return Response.success();
    }
}
    
