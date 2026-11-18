package wcg.yhm.ymmknowledgeexchange.auth.api.sms;

import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-22
 * @Description: 发送短信
 * @Version: 1.0
 */

@Component
@Slf4j
public class AliyunSmsHandle {

    @Resource
    private com.aliyun.dypnsapi20170525.Client client;

    /**
     * 发送短信
     * 参数详情可查询官方手册
     * https://dypns.console.aliyun.com/smsCertParamsConfig
     *
     * @param signName      签名名称 速通互联验证码
     * @param templateCode  模板配置code 登陆模板，修改模板等
     * @param phone         手机号
     * @param templateParam 验证码格式
     */
    public void sendMessage(String signName, String templateCode, String templateParam, String phone) {
        com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest sendSmsVerifyCodeRequest =
                new com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest()
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setPhoneNumber(phone)
                .setTemplateParam(templateParam);
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            log.info("=======> 开始发送短信 ⁽⁽ ◟(∗ ˊωˋ ∗)◞ ⁾⁾ , signName: [{}], templateCode: [{}], templateParam: [{}], 手机号: [{}]",
                    signName, templateCode, templateParam, phone);
            SendSmsVerifyCodeResponse sendSmsVerifyCodeResponse =
                    client.sendSmsVerifyCodeWithOptions(sendSmsVerifyCodeRequest, runtime);
            log.info("=======> 短信发送成功 (*´∀`)~♥ , response: [{}]", JsonUtil.toJsonString(sendSmsVerifyCodeResponse));
        } catch (Exception error) {
            log.error("短信发送失败 ( ´•̥̥̥ω•̥̥̥` ) , error");
        }

    }
}
    
