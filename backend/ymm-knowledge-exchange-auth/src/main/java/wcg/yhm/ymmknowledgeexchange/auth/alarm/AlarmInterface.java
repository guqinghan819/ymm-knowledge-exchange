package wcg.yhm.ymmknowledgeexchange.auth.alarm;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description:发送警报接口
 * @Version: 1.0
 */

public interface AlarmInterface {
    /**
     * 发送警报
     * @param message 警报消息
     * @return
     */
    boolean send(String message);
}
    
