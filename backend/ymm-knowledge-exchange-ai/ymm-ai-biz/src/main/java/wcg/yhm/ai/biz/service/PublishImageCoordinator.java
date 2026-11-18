package wcg.yhm.ai.biz.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.service.ImageGenerationService;
import wcg.yhm.ai.biz.agent.service.ImageGenerationService.ImageGenerationException;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.rpc.OssRpcService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.framework.context.utils.UserContextHolder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadLocalRandom;

/** Publishes each image independently. Model, download and upload retries never redo a successful stage. */
@Service
@Slf4j
public class PublishImageCoordinator {
    static final Duration IMAGE_BUDGET = Duration.ofSeconds(180);
    static final int PRIMARY_ATTEMPTS = 3; // initial request + two retries
    static final int TRANSFER_ATTEMPTS = 3;
    private static final Duration MODEL_CALL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration TRANSFER_TIMEOUT = Duration.ofSeconds(25);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @Resource private ImageGenerationService imageGenerationService;
    @Resource private OssRpcService ossRpcService;
    @Resource(name = "publishImageExecutor") private ThreadPoolTaskExecutor imageExecutor;

    public Batch start(List<String> prompts, Long userId, RecordingSink sink) {
        long deadline = System.nanoTime() + IMAGE_BUDGET.toNanos();
        List<Future<Outcome>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < prompts.size(); i++) {
                final int imageIndex = i + 1;
                final String prompt = prompts.get(i);
                tasks.add(imageExecutor.submit(() -> createOne(prompt, imageIndex, userId, sink, deadline)));
            }
        } catch (RuntimeException e) {
            tasks.forEach(task -> task.cancel(true));
            throw e;
        }
        Batch batch = new Batch(tasks, deadline);
        sink.addOnCancel(batch::cancel);
        if (sink.isEnded()) batch.cancel();
        return batch;
    }

    public List<String> await(Batch batch, RecordingSink sink) {
        for (Future<Outcome> task : batch.tasks) {
            if (sink.isEnded() || remaining(batch.deadlineNanos).isZero()) break;
            try {
                task.get(remaining(batch.deadlineNanos).toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                log.warn("发布配图任务异常", e.getCause());
            } catch (CancellationException ignored) {
                break;
            }
        }
        List<String> urls = new ArrayList<>();
        for (Future<Outcome> task : batch.tasks) {
            if (!task.isDone() || task.isCancelled()) continue;
            try {
                Outcome result = task.get();
                if (result.url != null) urls.add(result.url);
            } catch (Exception e) {
                log.warn("发布配图结果读取失败", e);
            }
        }
        batch.cancel();
        return urls;
    }

    Outcome createOne(String prompt, int imageIndex, Long userId, RecordingSink sink, long deadline) {
        long start = System.nanoTime();
        UserContextHolder.setUserId(userId);
        try {
            if (cancelled(sink, deadline)) return Outcome.failed();
            emit(sink, "- 正在生成配图 " + imageIndex + "/3。\n");
            String generatedUrl = generateWithRetry(prompt, imageIndex, sink, deadline);
            if (generatedUrl == null || cancelled(sink, deadline)) return Outcome.failed();
            byte[] bytes = downloadWithRetry(generatedUrl, imageIndex, sink, deadline);
            if (bytes == null || cancelled(sink, deadline)) return Outcome.failed();
            String uploaded = uploadWithRetry(bytes,
                    "publish-assistant-" + sink.taskId() + "-" + imageIndex + ".png",
                    imageIndex, sink, deadline);
            if (uploaded == null || cancelled(sink, deadline)) return Outcome.failed();
            emit(sink, "- 配图 " + imageIndex + "/3 已生成并上传。\n");
            log.info("发布配图完成: index={}, durationMs={}", imageIndex,
                    Duration.ofNanos(System.nanoTime() - start).toMillis());
            return new Outcome(uploaded);
        } finally {
            UserContextHolder.removeThreadLocal();
        }
    }

    private String generateWithRetry(String prompt, int index, RecordingSink sink, long deadline) {
        boolean tryBackup = false;
        for (int attempt = 1; attempt <= PRIMARY_ATTEMPTS && !cancelled(sink, deadline); attempt++) {
            try {
                return imageGenerationService.generatePublishImage(prompt,
                        shorter(MODEL_CALL_TIMEOUT, remaining(deadline)));
            } catch (ImageGenerationException e) {
                log.warn("发布配图模型失败: index={}, attempt={}, retryable={}, reason={}",
                        index, attempt, e.retryable(), e.getMessage());
                tryBackup = e.retryable() || e.modelUnavailable();
                if (!e.retryable()) break;
                if (attempt < PRIMARY_ATTEMPTS) {
                    emit(sink, "- 配图 " + index + " 生成失败，正在进行第 " + attempt + "/2 次重试。\n");
                    long delay = Math.max(e.retryAfter().toMillis(), attempt * 650L
                            + ThreadLocalRandom.current().nextLong(350L));
                    pause(delay, sink, deadline);
                }
            }
        }
        if (tryBackup && !cancelled(sink, deadline)) {
            emit(sink, "- 配图 " + index + " 主模型未成功，尝试备用模型。\n");
            try {
                return imageGenerationService.generatePublishFallbackImage(prompt,
                        shorter(MODEL_CALL_TIMEOUT, remaining(deadline)));
            } catch (ImageGenerationException e) {
                log.warn("发布配图备用模型失败: index={}, reason={}", index, e.getMessage());
            }
        }
        emit(sink, "- 配图 " + index + " 生成失败，继续处理其他图片。\n");
        return null;
    }

    private byte[] downloadWithRetry(String url, int index, RecordingSink sink, long deadline) {
        for (int attempt = 1; attempt <= TRANSFER_ATTEMPTS && !cancelled(sink, deadline); attempt++) {
            try {
                return download(url, shorter(TRANSFER_TIMEOUT, remaining(deadline)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.warn("发布配图下载失败: index={}, attempt={}", index, attempt, e);
                if (attempt < TRANSFER_ATTEMPTS) pause(attempt * 500L, sink, deadline);
            }
        }
        emit(sink, "- 配图 " + index + " 下载失败，继续处理其他图片。\n");
        return null;
    }

    private String uploadWithRetry(byte[] bytes, String fileName, int index,
                                   RecordingSink sink, long deadline) {
        for (int attempt = 1; attempt <= TRANSFER_ATTEMPTS && !cancelled(sink, deadline); attempt++) {
            try {
                String url = ossRpcService.uploadBytes(bytes, fileName);
                if (url != null && !url.isBlank()) return url;
                throw new IllegalStateException("上传未返回图片地址");
            } catch (Exception e) {
                log.warn("发布配图上传失败: index={}, attempt={}", index, attempt, e);
                if (attempt < TRANSFER_ATTEMPTS) pause(attempt * 500L, sink, deadline);
            }
        }
        emit(sink, "- 配图 " + index + " 上传失败，继续处理其他图片。\n");
        return null;
    }

    byte[] download(String url, Duration timeout) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200 || response.body() == null || response.body().length == 0
                || response.body().length > 10 * 1024 * 1024) {
            throw new IOException("图片下载响应无效: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void pause(long millis, RecordingSink sink, long deadline) {
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (!cancelled(sink, deadline) && System.nanoTime() < end) {
            try { Thread.sleep(Math.min(250, Math.max(1, TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime())))); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }
    private boolean cancelled(RecordingSink sink, long deadline) {
        return sink.isEnded() || Thread.currentThread().isInterrupted() || remaining(deadline).isZero();
    }
    private Duration remaining(long deadline) {
        return Duration.ofNanos(Math.max(0, deadline - System.nanoTime()));
    }
    private Duration shorter(Duration limit, Duration remaining) {
        return remaining.compareTo(limit) < 0 ? remaining : limit;
    }
    private void emit(Sinks.Many<String> sink, String message) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, message);
    }

    public record Outcome(String url) {
        static Outcome failed() { return new Outcome(null); }
    }
    public record Batch(List<Future<Outcome>> tasks, long deadlineNanos) {
        public void cancel() { tasks.forEach(task -> { if (!task.isDone()) task.cancel(true); }); }
    }
}
