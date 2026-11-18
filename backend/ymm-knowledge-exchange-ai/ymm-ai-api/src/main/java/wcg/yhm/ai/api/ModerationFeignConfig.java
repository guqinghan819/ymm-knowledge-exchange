package wcg.yhm.ai.api;

import feign.Request;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public class ModerationFeignConfig {
    @Bean
    public Request.Options moderationRequestOptions() {
        // Up to eight images at 25 s each, followed by text at 35 s.
        return new Request.Options(3, TimeUnit.SECONDS, 250, TimeUnit.SECONDS, true);
    }
}
