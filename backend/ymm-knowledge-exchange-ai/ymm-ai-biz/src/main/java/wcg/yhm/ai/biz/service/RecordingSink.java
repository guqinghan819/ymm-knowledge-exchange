package wcg.yhm.ai.biz.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.domain.pojo.AiMessage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One request owns one sink and one database row, including all PPT stages. */
@Slf4j
public final class RecordingSink implements Sinks.Many<String> {
    private final Sinks.Many<String> delegate = Sinks.many().unicast()
            .onBackpressureBuffer(new LinkedBlockingQueue<>(256));
    private final AiMessage message;
    private final Consumer<AiMessage> persist;
    private final Map<String, Object> metadata;
    private final StringBuilder answer = new StringBuilder();
    private final StringBuilder thinking = new StringBuilder();
    private final AtomicBoolean started = new AtomicBoolean();
    private boolean ended;
    private Runnable onFinished = () -> { };
    private Runnable cancelGeneration = () -> { };

    public RecordingSink(AiMessage message, Map<String, Object> metadata, Consumer<AiMessage> persist) {
        this.message = message;
        this.metadata = new LinkedHashMap<>(metadata);
        this.persist = persist;
    }

    public Flux<String> start(Runnable action) {
        return asFlux().doOnRequest(demand -> {
            if (isEnded() || !started.compareAndSet(false, true)) return;
            try { action.run(); } catch (Throwable error) { tryEmitError(error); }
        });
    }

    public String taskId() { return message.getMessageId(); }
    public Long userId() { return message.getUserId(); }
    public String conversationId() { return message.getConversationId(); }
    public synchronized boolean isEnded() { return ended; }
    public synchronized void onFinished(Runnable action) { onFinished = action; }
    public synchronized void onCancel(Runnable action) { cancelGeneration = action; }
    public synchronized void addOnCancel(Runnable action) {
        Runnable previous = cancelGeneration;
        cancelGeneration = () -> { previous.run(); action.run(); };
    }
    public synchronized void metadata(String key, Object value) {
        if (!ended) metadata.put(key, value);
    }

    @Override
    public synchronized Sinks.EmitResult tryEmitNext(String event) {
        if (ended) return Sinks.EmitResult.FAIL_TERMINATED;
        JSONObject data = JSON.parseObject(event.trim());
        String type = data.getString("type");
        Object content = data.get("content");
        String text = content == null ? "" : content.toString();
        switch (type == null ? "" : type) {
            case "text" -> answer.append(text);
            case "thinking" -> {
                if (content instanceof JSONObject object) text = object.getString("text");
                if (text != null) thinking.append(text);
            }
            case "step" -> thinking.append("- ").append(text).append('\n');
            case "reference" -> message.setReference(content instanceof String ? text : JSON.toJSONString(content));
            case "recommend" -> message.setRecommend(JSON.toJSONString(content));
            case "publishOptions", "publishThemeOptions" -> metadata.put(type, content);
            case "error" -> {
                String error = text.isBlank() ? data.getString("message") : text;
                message.setErrorMessage(error == null || error.isBlank() ? "生成失败" : error);
            }
            // A success signal is sent only after the database update succeeds.
            case "complete" -> { return Sinks.EmitResult.OK; }
            default -> { }
        }
        Sinks.EmitResult result = delegate.tryEmitNext(event);
        if (result.isFailure()) stop("DISCONNECTED");
        return result;
    }

    public synchronized void stop(String status) {
        if (ended) return;
        finish(status);
        cancelGeneration.run();
    }

    private synchronized Sinks.EmitResult finish(String status) {
        if (ended) return Sinks.EmitResult.FAIL_TERMINATED;
        ended = true;
        message.setStatus(message.getErrorMessage() == null ? status : "FAILED");
        message.setAnswer(answer.toString());
        message.setThinking(thinking.toString());
        message.setMetadata(JSON.toJSONString(metadata));
        try {
            persist.accept(message);
            delegate.tryEmitNext(JSON.toJSONString(Map.of("type", "complete", "content", "done",
                    "status", message.getStatus(), "messageId", taskId())) + "\n\n");
        } catch (RuntimeException e) {
            log.error("对话保存失败: userId={}, conversationId={}, messageId={}",
                    userId(), conversationId(), taskId(), e);
            delegate.tryEmitNext(event("error", "对话记录保存失败，请稍后查看历史确认；请勿重复提交发布请求"));
        } finally {
            try { onFinished.run(); }
            finally { delegate.tryEmitComplete(); }
        }
        return Sinks.EmitResult.OK;
    }

    public static String event(String type, Object content) {
        return JSON.toJSONString(Map.of("type", type, "content", content)) + "\n\n";
    }

    @Override public Sinks.EmitResult tryEmitComplete() { return finish("COMPLETED"); }
    @Override public synchronized Sinks.EmitResult tryEmitError(Throwable error) {
        if (ended) return Sinks.EmitResult.FAIL_TERMINATED;
        tryEmitNext(event("error", error.getMessage() == null ? "生成失败" : error.getMessage()));
        return finish("FAILED");
    }
    @Override public void emitNext(String value, Sinks.EmitFailureHandler handler) {
        Sinks.EmitResult result;
        do { result = tryEmitNext(value); } while (result.isFailure() && handler.onEmitFailure(SignalType.ON_NEXT, result));
    }
    @Override public void emitComplete(Sinks.EmitFailureHandler handler) { tryEmitComplete(); }
    @Override public void emitError(Throwable error, Sinks.EmitFailureHandler handler) { tryEmitError(error); }
    @Override public int currentSubscriberCount() { return delegate.currentSubscriberCount(); }
    @Override public Flux<String> asFlux() {
        return delegate.asFlux().doOnCancel(() -> stop("DISCONNECTED"));
    }
    @Override public Object scanUnsafe(Attr key) { return delegate.scanUnsafe(key); }
}
