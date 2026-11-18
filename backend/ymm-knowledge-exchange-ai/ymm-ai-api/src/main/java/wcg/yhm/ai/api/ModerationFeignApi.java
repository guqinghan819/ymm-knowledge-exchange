package wcg.yhm.ai.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ymm-ai-biz", contextId = "moderationFeignApi", configuration = ModerationFeignConfig.class)
public interface ModerationFeignApi {
    @PostMapping("/ai/internal/moderation/review")
    ModerationResult review(@RequestBody ModerationRequest request);
}
