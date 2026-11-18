package wcg.yhm.ai.biz.agent.service;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-15
 * @Description:
 * @Version: 1.0
 */

@Slf4j
@Service
public class ImageGenerationService {
    // Qwen API配置
    private static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    @Value("${langchain4j.dashscope.api-key}")
    private String apiKey;

    @Value("${langchain4j.dashscope.image-model-name}")
    private String imageModelName;

    @Value("${langchain4j.dashscope.publish-image-model-name}")
    private String publishImageModelName;

    @Value("${langchain4j.dashscope.publish-image-fallback-model-name}")
    private String publishImageFallbackModelName;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();


    /**
     * 使用通义千问生成图像（multimodal-generation 同步接口）
     */
    public String generateWithQwen(String prompt) {
        return generateWithQwenModel(prompt, imageModelName);
    }

    /**
     * 使用配置的发布助手模型生成配图。
     */
    public String generateWithQwenImage30(String prompt) {
        return generateWithQwenModel(prompt, publishImageModelName);
    }

    public String generatePublishImage(String prompt, Duration timeout) throws ImageGenerationException {
        return generatePublishImage(prompt, publishImageModelName, timeout);
    }

    public String generatePublishFallbackImage(String prompt, Duration timeout) throws ImageGenerationException {
        return generatePublishImage(prompt, publishImageFallbackModelName, timeout);
    }

    /** Publish assistant keeps failures visible so callers can make bounded retry decisions. */
    public String generatePublishImage(String prompt, String model, Duration timeout) throws ImageGenerationException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", Map.of("messages", new Object[]{Map.of("role", "user",
                "content", new Object[]{Map.of("text", prompt)})}));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("negative_prompt", "低分辨率，低画质，肢体畸形，文字模糊，水印，logo");
        parameters.put("prompt_extend", false);
        parameters.put("watermark", false);
        parameters.put("size", "1664*928");
        if (model.startsWith("qwen-image-3.0")) parameters.put("enable_thinking", false);
        body.put("parameters", parameters);
        HttpRequest request = HttpRequest.newBuilder(URI.create(QWEN_API_URL))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();
        long start = System.nanoTime();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("发布配图模型调用: model={}, status={}, durationMs={}", model,
                    response.statusCode(), Duration.ofNanos(System.nanoTime() - start).toMillis());
            if (response.statusCode() != 200) {
                String code = "";
                try { code = JSON.parseObject(response.body()).getString("code"); }
                catch (RuntimeException ignored) { /* The status code still determines retryability. */ }
                int status = response.statusCode();
                boolean retryable = status == 408 || status == 429 || status >= 500;
                boolean modelUnavailable = status == 404 || "ModelNotFound".equalsIgnoreCase(code);
                Duration retryAfter = response.headers().firstValue("Retry-After").map(value -> {
                    try { return Duration.ofSeconds(Math.max(0, Long.parseLong(value))); }
                    catch (NumberFormatException ignored) { return Duration.ZERO; }
                }).orElse(Duration.ZERO);
                throw new ImageGenerationException("模型请求失败: HTTP " + status + " / " + code,
                        retryable, modelUnavailable, retryAfter);
            }
            JSONObject parsed;
            try { parsed = JSON.parseObject(response.body()); }
            catch (RuntimeException e) {
                throw new ImageGenerationException("模型响应格式无效", true, false, Duration.ZERO, e);
            }
            JSONObject output = parsed == null ? null : parsed.getJSONObject("output");
            if (output != null && output.getJSONArray("choices") != null) {
                for (Object choiceValue : output.getJSONArray("choices")) {
                    JSONObject choice = (JSONObject) choiceValue;
                    JSONObject message = choice.getJSONObject("message");
                    if (message == null || message.getJSONArray("content") == null) continue;
                    for (Object contentValue : message.getJSONArray("content")) {
                        JSONObject content = (JSONObject) contentValue;
                        String url = content.getString("image");
                        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) return url;
                    }
                }
            }
            throw new ImageGenerationException("模型未返回有效图片地址", true, false, Duration.ZERO);
        } catch (IOException e) {
            throw new ImageGenerationException("模型网络调用失败", true, false, Duration.ZERO, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ImageGenerationException("模型调用已取消", false, false, Duration.ZERO, e);
        }
    }

    public static class ImageGenerationException extends Exception {
        private final boolean retryable;
        private final boolean modelUnavailable;
        private final Duration retryAfter;

        public ImageGenerationException(String message, boolean retryable, boolean modelUnavailable,
                                        Duration retryAfter) {
            super(message);
            this.retryable = retryable;
            this.modelUnavailable = modelUnavailable;
            this.retryAfter = retryAfter;
        }
        public ImageGenerationException(String message, boolean retryable, boolean modelUnavailable,
                                        Duration retryAfter, Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
            this.modelUnavailable = modelUnavailable;
            this.retryAfter = retryAfter;
        }
        public boolean retryable() { return retryable; }
        public boolean modelUnavailable() { return modelUnavailable; }
        public Duration retryAfter() { return retryAfter; }
    }

    private String generateWithQwenModel(String prompt, String model) {
        try {
            log.info("Qwen图像生成开始: model={}, promptLength={}", model, prompt == null ? 0 : prompt.length());

            // 构建请求参数
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            // input 使用 messages 格式
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("text", prompt);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", new Object[]{textContent});

            Map<String, Object> input = new HashMap<>();
            input.put("messages", new Object[]{userMessage});
            requestBody.put("input", input);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("negative_prompt", "低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲。");
            parameters.put("prompt_extend", true);
            if (model.startsWith("qwen-image-3.0")) parameters.put("enable_thinking", false);
            parameters.put("watermark", false);
            parameters.put("size", "1664*928");
            requestBody.put("parameters", parameters);

            // 创建HTTP请求
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(QWEN_API_URL))
                    .timeout(Duration.ofMinutes(5));

            // 添加请求头
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.header("Authorization", "Bearer " + apiKey);

            // 添加请求体
            String bodyStr = JSON.toJSONString(requestBody);
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyStr));

            HttpRequest request = requestBuilder.build();

            // 发送请求
            HttpResponse<String> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString());
            log.info("Qwen图像生成请求完成: model={}, status={}", model, response.statusCode());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = JSON.parseObject(response.body());
                log.debug("Qwen图像生成响应: {}", jsonResponse);

                // 从响应中直接获取图片URL
                JSONObject output = jsonResponse.getJSONObject("output");
                if (output != null && output.containsKey("choices")) {
                    com.alibaba.fastjson2.JSONArray choices = output.getJSONArray("choices");
                    if (choices != null && choices.size() > 0) {
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject message = choice.getJSONObject("message");
                        if (message != null && message.containsKey("content")) {
                            com.alibaba.fastjson2.JSONArray contents = message.getJSONArray("content");
                            if (contents != null && contents.size() > 0) {
                                JSONObject content = contents.getJSONObject(0);
                                if (content.containsKey("image")) {
                                    String imageUrl = content.getString("image");
                                    log.info("Qwen图像生成成功: model={}, url={}", model, imageUrl);
                                    return imageUrl;
                                }
                            }
                        }
                    }
                }
            } else {
                log.error("Qwen HTTP请求失败: model={}, status={}, response={}", model, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Qwen图像生成失败: model={}", model, e);
        }
        return null;
    }

}
    
