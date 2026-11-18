package wcg.yhm.ymmknowledgeexchange.auth.alarm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wcg.yhm.ymmknowledgeexchange.auth.alarm.impl.MailAlarmImpl;
import wcg.yhm.ymmknowledgeexchange.auth.alarm.impl.SmsAlarmImpl;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description:
 * @Version: 1.0
 */

@Configuration
@RefreshScope
public class AlarmConfig {
    @Value("${alarm.type}")
    private String type;

    @Bean
    @RefreshScope
    public AlarmInterface alarmHelper() {
        // 根据配置文件中的告警类型，初始化选择不同的告警实现类
        if ("Sms".equals(type)) {
            return new SmsAlarmImpl();
        } else if ("mail".equals(type)) {
            return new MailAlarmImpl();
        } else {
            throw new IllegalArgumentException("错误的告警类型...");
        }
    }
}
    
