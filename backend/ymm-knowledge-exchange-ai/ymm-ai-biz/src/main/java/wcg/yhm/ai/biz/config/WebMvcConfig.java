package wcg.yhm.ai.biz.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 异步支持配置 — 为 Flux/SSE 流式响应提供线程池，
 * 替代默认的 SimpleAsyncTaskExecutor。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(300_000); // SSE 长连接，5 分钟超时
    }
}
