package wcg.yhm.ymm.data.align.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.data.align.constants.TableConstant;
import wcg.yhm.ymm.data.align.mapper.DeleteTableMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-17
 * @Description: 删除日增量表
 * @Version: 1.0
 */

@Component
public class DeleteTableXxlJob {
    @Value("${table.shards}")
    private int shards;
    @Resource
    private DeleteTableMapper deleteTableMapper;

    @XxlJob("deleteTableJobHandler")
    public void deleteTableJobHandler() {
        XxlJobHelper.log("## 开始删除最近一个月增量数据表...");
        // 今日
        LocalDate today = LocalDate.now();

        // 日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        LocalDate startDate = today;
        // 从昨天开始往前推一个月
        LocalDate endDate = today.minusMonths(1);

        // 循环最近一个月的日期，不包括今天
        while (startDate.isAfter(endDate)) {
            // 往前推一天
            startDate = startDate.minusDays(1);
            // 日期字符串
            String date = startDate.format(formatter);

            for (int hashKey = 0; hashKey < shards; hashKey++) {
                // 表名后缀
                String tableNameSuffix = TableConstant.buildTableNameSuffix(date, hashKey);
                XxlJobHelper.log("删除表后缀: {}", tableNameSuffix);

                // 删除表
                deleteTableMapper.deleteDataAlignFollowingCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignFansCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNoteCollectCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignUserCollectCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignUserLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNoteLikeCountTempTable(tableNameSuffix);
                deleteTableMapper.deleteDataAlignNotePublishCountTempTable(tableNameSuffix);
            }
        }
        XxlJobHelper.log("## 结束删除最近一个月的日增量临时表");
    }
}
    
