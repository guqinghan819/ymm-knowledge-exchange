package wcg.yhm.ymm.data.align.constants;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-16
 * @Description: 表常量
 * @Version: 1.0
 */

public class TableConstant {

    /**
     * 表名分隔符
     */
    public static final String TABLE_NAME_SEPARATOR = "_";

    /**
     * 构建完整表名后缀
     * @param date 日期
     * @param hashKey 分片序号
     * @return 表名
     */
    public static String buildTableNameSuffix(String date, long hashKey) {
        return date + TABLE_NAME_SEPARATOR + hashKey;
    }
}
    
