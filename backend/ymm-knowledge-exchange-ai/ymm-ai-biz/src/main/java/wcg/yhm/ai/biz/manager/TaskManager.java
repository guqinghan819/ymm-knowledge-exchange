package wcg.yhm.ai.biz.manager;

import dev.langchain4j.model.chat.response.StreamingHandle;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.service.RecordingSink;
import java.util.concurrent.ConcurrentHashMap;

/** Callbacks use turn IDs; only stop requests resolve a conversation alias. */
@Component
public class TaskManager {
    private final ConcurrentHashMap<String, TaskContext> tasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> conversations = new ConcurrentHashMap<>();

    public synchronized void register(String taskId, Sinks.Many<String> sink) {
        String alias = sink instanceof RecordingSink recording
                ? recording.userId() + ":" + recording.conversationId() : taskId;
        String previous = conversations.get(alias);
        if (previous != null) cancel(previous);
        TaskContext context = new TaskContext(alias, sink);
        tasks.put(taskId, context);
        conversations.put(alias, taskId);
        if (sink instanceof RecordingSink recording) {
            recording.onFinished(() -> removeTask(taskId));
            recording.onCancel(() -> {
                context.cancelled = true;
                if (context.handle != null) context.handle.cancel();
            });
        }
    }
    public void bindHandle(String taskId, StreamingHandle handle) {
        TaskContext context = tasks.get(taskId);
        if (context == null) { handle.cancel(); return; }
        context.handle = handle;
        if (context.cancelled || (context.sink instanceof RecordingSink sink && sink.isEnded())) handle.cancel();
    }
    public boolean cancel(String id) {
        if (id == null) return false;
        String taskId = conversations.getOrDefault(id, id);
        TaskContext context = tasks.get(taskId);
        if (context == null) return false;
        context.cancelled = true;
        if (context.sink instanceof RecordingSink sink) sink.stop("STOPPED");
        else context.sink.tryEmitComplete();
        if (context.handle != null) context.handle.cancel();
        removeTask(taskId);
        return true;
    }
    public void cancelUser(Long userId) {
        conversations.keySet().stream().filter(key -> key.startsWith(userId + ":")).toList().forEach(this::cancel);
    }
    public boolean contains(String taskId) { return tasks.containsKey(taskId); }
    public boolean isCancelled(String taskId) {
        TaskContext context = tasks.get(taskId);
        return context == null || context.cancelled
                || (context.sink instanceof RecordingSink sink && sink.isEnded());
    }
    public void removeTask(String taskId) {
        TaskContext removed = tasks.remove(taskId);
        if (removed != null) conversations.remove(removed.alias, taskId);
    }
    private static class TaskContext {
        final String alias;
        final Sinks.Many<String> sink;
        volatile StreamingHandle handle;
        volatile boolean cancelled;
        TaskContext(String alias, Sinks.Many<String> sink) { this.alias = alias; this.sink = sink; }
    }
}
