package wcg.yhm.user.biz.rpc;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.tokens.Token;
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
     * ymm ID 标识
     */
    private static final String BIZ_TAG_YMM_ID = "leaf-segment-ymm-id";

    private static final String BIZ_TAG_USER_ID = "leaf-segment-user-id";
    /**
     * 生成ymm id
     * @return
     */
    public String getYmmId() {
        return idGeneratorFeignApi.getSegmentId(BIZ_TAG_YMM_ID);
    }

    public String getUserId() {
        return idGeneratorFeignApi.getSegmentId(BIZ_TAG_USER_ID);
    }
}
    
