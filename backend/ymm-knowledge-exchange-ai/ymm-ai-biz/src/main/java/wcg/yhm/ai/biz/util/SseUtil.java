package wcg.yhm.ai.biz.util;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description:
 * @Version: 1.0
 */

@Slf4j
public class SseUtil {

    public static boolean sendEvent(Sinks.Many<String> sink, String eventType, Object content) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", eventType);
            data.put("content", content);

            String jsonData = wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil.toJsonString(data);
            String event = String.format("%s\n\n", jsonData);

            Sinks.EmitResult result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.warn("SSE 发送失败，客户端已断开: eventType={}, result={}", eventType, result);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("发送事件失败: eventType={}", eventType, e);
            return false;
        }
    }
}
    
