package wcg.yhm.comment.biz.rpc;

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
     * 评论id生成
     * @return
     */
    public String generateCommentId() {
        return idGeneratorFeignApi.getSegmentId("leaf-segment-comment-id");
    }


}
    
