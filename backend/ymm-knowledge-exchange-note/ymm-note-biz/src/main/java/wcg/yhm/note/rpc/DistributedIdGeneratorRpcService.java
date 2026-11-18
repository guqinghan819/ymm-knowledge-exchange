package wcg.yhm.note.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import wcg.yhm.distributed.id.api.IdGeneratorFeignApi;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-22
 * @Description: 分布式id生成
 * @Version: 1.0
 */

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private IdGeneratorFeignApi idGeneratorFeignApi;

    /**
     * 雪花算法随便生成cassandra的分区键
     * @return
     */
    public String getPartitionKey() {
        return idGeneratorFeignApi.getSnowflakeId("yhm666"); //随便填
    }


}
    
