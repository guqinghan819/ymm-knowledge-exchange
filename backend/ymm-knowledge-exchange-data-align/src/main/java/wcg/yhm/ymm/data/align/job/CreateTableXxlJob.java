package wcg.yhm.ymm.data.align.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.data.align.constants.TableConstant;
import wcg.yhm.ymm.data.align.mapper.CreateTableMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-16
 * @Description: 创建表定时任务类
 * @Version: 1.0
 */

@Component
//@RefreshScope  xxl-job 3.3.2 这个版本好像对这个注解不支持(瞎说的)
public class CreateTableXxlJob {
    @Value("${table.shards}")
    private int shards;

    @Resource
    private CreateTableMapper createTableMapper;

    /**
     * bean模式
     */
    @XxlJob("createTableJobHandler") //保持和界面中设置的一致
    public void createTableJobHandler() {
        XxlJobHelper.log("## 开始初始化明日增量数据表...");
        String date = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        XxlJobHelper.log("## 开始创建增量表, 日期: {}", date);
        if (shards > 0) {
            for (int hashKey = 0; hashKey < shards; hashKey++) {
                String tableNameSuffix = TableConstant.buildTableNameSuffix(date, hashKey);
                //创建表
                createTableMapper.createDataAlignFollowingCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignFansCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNoteCollectCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignUserCollectCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignUserLikeCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNoteLikeCountTempTable(tableNameSuffix);
                createTableMapper.createDataAlignNotePublishCountTempTable(tableNameSuffix);
            }
        }
        XxlJobHelper.log("## 增量表创建成功");
    }
}
    
