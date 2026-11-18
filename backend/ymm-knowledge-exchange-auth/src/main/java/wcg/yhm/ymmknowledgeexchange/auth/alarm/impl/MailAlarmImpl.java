package wcg.yhm.ymmknowledgeexchange.auth.alarm.impl;

import lombok.extern.slf4j.Slf4j;
import wcg.yhm.ymmknowledgeexchange.auth.alarm.AlarmInterface;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 发送邮件警报
 * @Version: 1.0
 */

@Slf4j
public class MailAlarmImpl implements AlarmInterface {
    @Override
    public boolean send(String message) {
        log.info("============== mail 警报： {}", message);
        return true;
    }
}
    
