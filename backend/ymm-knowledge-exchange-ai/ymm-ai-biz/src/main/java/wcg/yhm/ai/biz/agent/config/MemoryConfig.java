package wcg.yhm.ai.biz.agent.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MemoryConfig {

    private static final int MAX_SIZE = 500;
    private static final int EXPIRE_MINUTES = 30;

    private final Cache<Object, MessageWindowChatMemory> cache = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterAccess(Duration.ofMinutes(EXPIRE_MINUTES))
            .build();

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> cache.get(memoryId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(14)
                        .build());
    }
}