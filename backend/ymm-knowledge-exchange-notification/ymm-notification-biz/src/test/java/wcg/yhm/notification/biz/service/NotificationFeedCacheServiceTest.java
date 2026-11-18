package wcg.yhm.notification.biz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationFeedCacheServiceTest {
    private RedisTemplate<String, Object> redisTemplate;
    private NotificationFeedCacheService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        service = new NotificationFeedCacheService();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
    }

    @Test
    void redisFailureMakesFeedMissSoCallerCanFallbackToMysql() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new IllegalStateException("redis unavailable"));

        assertThat(service.firstPageIds(22L, 20)).isNull();
    }

    @Test
    void redisFailureDoesNotBreakUnreadCacheInvalidation() {
        when(redisTemplate.delete(anyString())).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatCode(() -> service.invalidateUnread(22L)).doesNotThrowAnyException();
    }
}
