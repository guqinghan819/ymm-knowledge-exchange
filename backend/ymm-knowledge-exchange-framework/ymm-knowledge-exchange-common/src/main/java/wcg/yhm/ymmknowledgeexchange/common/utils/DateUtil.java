package wcg.yhm.ymmknowledgeexchange.common.utils;

import wcg.yhm.ymmknowledgeexchange.common.constant.DateConstant;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description: 日期工具类
 * @Version: 1.0
 */

public class DateUtil {
    /**
     * 转换时间戳
     * @return 当前时间戳
     */
    public static long localDateTime2TimeStamp(LocalDateTime localDateTime) {
        //如果是在用于时间排序的场景下，时区不同会有时差这是没问题的，别的场景需要考察
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * LocalDateTime转String
     * @param localDateTime 当前时间
     * @return
     */
    public static String localDateTime2String(LocalDateTime localDateTime) {
        return localDateTime.format(DateConstant.DATE_FORMAT_Y_M_D_H_M_S);
    }

    /**
     * LocalDateTime 转友好的相对时间字符串
     * @param dateTime
     * @return
     */
    public static String formatRelativeTime(LocalDateTime dateTime) {
        // 当前时间
        LocalDateTime now = LocalDateTime.now();

        // 计算与当前时间的差距
        long daysDiff = ChronoUnit.DAYS.between(dateTime, now);
        long hoursDiff = ChronoUnit.HOURS.between(dateTime, now);
        long minutesDiff = ChronoUnit.MINUTES.between(dateTime, now);

        if (daysDiff < 1) {  // 如果是今天
            if (hoursDiff < 1) {  // 如果是几分钟前
                return minutesDiff + "分钟前";
            } else {  // 如果是几小时前
                return hoursDiff + "小时前";
            }
        } else if (daysDiff == 1) {  // 如果是昨天
            return "昨天 " + dateTime.format(DateConstant.DATE_FORMAT_H_M);
        } else if (daysDiff < 7) {  // 如果是最近一周
            return daysDiff + "天前";
        } else if (dateTime.getYear() == now.getYear()) {  // 如果是今年
            return dateTime.format(DateConstant.DATE_FORMAT_M_D);
        } else {  // 如果是去年或更早
            return dateTime.format(DateConstant.DATE_FORMAT_Y_M_D);
        }
    }
}
    
