package wcg.yhm.notification.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import wcg.yhm.notification.biz.domain.po.NotificationEventLog;

public interface NotificationEventLogMapper extends BaseMapper<NotificationEventLog> {
    @Insert("INSERT IGNORE INTO notification_event_log(event_id, event_type, create_time) " +
            "VALUES(#{eventId}, #{eventType}, #{createTime})")
    int insertIgnore(NotificationEventLog eventLog);
}
