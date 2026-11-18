package wcg.yhm.ai.biz.agent.pptx.strategy.impl;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.pptx.assistant.SchemaAssistant;
import wcg.yhm.ai.biz.agent.pptx.model.FieldData;
import wcg.yhm.ai.biz.agent.pptx.model.PptSchema;
import wcg.yhm.ai.biz.agent.pptx.model.Slide;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStateStrategy;
import wcg.yhm.ai.biz.agent.pptx.strategy.PptStatusStrategyFactory;
import wcg.yhm.ai.biz.agent.service.ImageGenerationService;
import wcg.yhm.ai.biz.constants.EventType;
import wcg.yhm.ai.biz.rpc.OssRpcService;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.domain.pojo.AiPptTemplate;
import wcg.yhm.ai.biz.service.IAiPptInstService;
import wcg.yhm.ai.biz.service.IAiPptTemplateService;
import wcg.yhm.ai.biz.util.SseUtil;
import wcg.yhm.ai.biz.service.RecordingSink;
import wcg.yhm.framework.context.utils.UserContextHolder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: 结构生成策略
 * @Version: 1.0
 */

@Component
@Slf4j
public class SchemaStrategy implements PptStateStrategy {

    private static final PptInstanceStatus TARGET_STATUS = PptInstanceStatus.RENDER;

    @Resource
    private IAiPptTemplateService aiPptTemplateService;

    @Resource
    private IAiPptInstService aiPptInstService;

    @Resource
    private SchemaAssistant schemaAssistant;

    @Resource
    @Lazy
    private PptStatusStrategyFactory pptStatusStrategyFactory;

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private OssRpcService ossRpcService;

    @Override
    public void execute(AiPptInst inst, Sinks.Many<String> sink, String query, StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在设计PPT详细内容...\n");

        String templateCode = inst.getTemplateCode();
        AiPptTemplate template = aiPptTemplateService.getByCode(templateCode);
        if (template == null) {
            log.error("模板不存在: templateCode={}", templateCode);
            String errorMsg = "模板不存在: " + templateCode;
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, errorMsg);
            inst.setErrorMsg(errorMsg);
            inst.setStatusEnum(PptInstanceStatus.SCHEMA);
            aiPptInstService.updateById(inst);
            // TODO: 跳转到失败策略
            // pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(new RuntimeException(errorMsg));
            return;
        }

        String templateSchema = template.getTemplateSchema();
        String outline = inst.getOutline();
        log.info("Schema生成开始 - instId: {}, template: {}", inst.getId(), template.getTemplateName());

        try {
            String json = schemaAssistant.create(templateSchema, outline);

            // 将复合JSON解析为PptSchema，同时完成结构合法性校验
            PptSchema pptSchema = JSON.parseObject(json, PptSchema.class);

            // 先生成图片素材（回填pptSchema中的imageUrl）
            processImageGeneration(pptSchema, sink, inst.getConversationId(), inst.getUserId());

            // 图片URL已回填到pptSchema，重新序列化并落库
            String pptSchemaJson = JSON.toJSONString(pptSchema);
            inst.setPptSchema(pptSchemaJson);
            inst.setStatusEnum(TARGET_STATUS);
            aiPptInstService.updateById(inst);
            log.info("Schema生成完成，实例 {} 状态: {} -> {}", inst.getId(), inst.getStatus(), TARGET_STATUS);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "\n✅ PPT内容设计完成，即将开始渲染...\n");
            pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
        } catch (Exception e) {
            log.error("Schema生成异常: instId={}", inst.getId(), e);
            // 失败时不回退状态，只更新错误信息，转到 FAILED
            String errorMsg = "Schema生成失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, errorMsg);
            inst.setErrorMsg(errorMsg);
            inst.setStatusEnum(PptInstanceStatus.SCHEMA);
            aiPptInstService.updateById(inst);
            // TODO: 跳转到失败策略
            // pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(e);
        }
    }

    /**
     * 修改模式：根据用户的修改需求重新生成 Schema
     */
    public void executeWithModify(AiPptInst inst, Sinks.Many<String> sink, String query,
                                   StringBuilder thinkingBuffer) {
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在根据修改需求重新设计PPT内容...\n");

        String currentSchema = inst.getPptSchema();
        log.info("Schema修改开始 - instId: {}", inst.getId());

        try {
            String json = schemaAssistant.modify(query, currentSchema);

            PptSchema pptSchema = JSON.parseObject(json, PptSchema.class);

            // 图片生成（仅处理url为空的image/background字段）
            processImageGeneration(pptSchema, sink, inst.getConversationId(), inst.getUserId());

            String pptSchemaJson = JSON.toJSONString(pptSchema);
            inst.setPptSchema(pptSchemaJson);
            inst.setStatusEnum(TARGET_STATUS);
            aiPptInstService.updateById(inst);
            log.info("Schema修改完成，实例 {} 状态: {} -> {}", inst.getId(), inst.getStatus(), TARGET_STATUS);
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "\n✅ PPT内容修改完成，即将开始渲染...\n");
            pptStatusStrategyFactory.executeNextState(inst, sink, query, thinkingBuffer);
        } catch (Exception e) {
            log.error("Schema修改异常: instId={}", inst.getId(), e);
            String errorMsg = "Schema修改失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            if (sink instanceof RecordingSink recording && recording.isEnded()) return;
            SseUtil.sendEvent(sink, EventType.TYPE_ERROR, errorMsg);
            inst.setErrorMsg(errorMsg);
            inst.setStatusEnum(PptInstanceStatus.SCHEMA);
            aiPptInstService.updateById(inst);
            pptStatusStrategyFactory.executeFailedState(inst, sink, query, thinkingBuffer);
            sink.tryEmitError(e);
        }
    }

    /**
     * 处理图片生成
     */
    private void processImageGeneration(PptSchema pptSchema, Sinks.Many<String> sink, String conversationId, Long userId) {
        if (pptSchema.getSlides() == null) {
            return;
        }
        // 首先收集所有需要生成图片的字段
        List<ImageGenerationTask> tasks = new ArrayList<>();
        for (Slide slide : pptSchema.getSlides()) {
            if (slide.getData() == null) {
                continue;
            }
            for (Map.Entry<String, FieldData> entry : slide.getData().entrySet()) {
                String key = entry.getKey();
                FieldData fieldData = entry.getValue();
                if (fieldData == null) {
                    continue;
                }
                String type = fieldData.getType();
                // 只处理image和background类型
                if (!"image".equalsIgnoreCase(type) && !"background".equalsIgnoreCase(type)) {
                    continue;
                }
                // 如果url是真实的OSS地址（非AI生成的假地址），跳过
                if (fieldData.getUrl() != null
                        && fieldData.getUrl().startsWith("http")
                        && !fieldData.getUrl().contains("example.com")) {
                    continue;
                }
                // url为空或为假地址，需要用content作为提示词生成图片
                fieldData.setUrl("");
                String prompt = fieldData.getContent();
                if (prompt == null || prompt.isEmpty()) {
                    continue;
                }
                tasks.add(new ImageGenerationTask(key, fieldData, prompt, slide));
            }
        }
        if (tasks.isEmpty()) {
            return;
        }
        int total = tasks.size();
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "✅PPT内容设计完成，开始生成图片素材\n");
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "共需生成 " + total + " 张图片，开始生成...\n");

        // 逐个生成图片
        for (int i = 0; i < tasks.size(); i++) {
            ImageGenerationTask task = tasks.get(i);
            int current = i + 1;

            if (sink instanceof RecordingSink recording && recording.isEnded()) return;

            SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "正在生成图片 (" + current + "/" + total + ")... \n");
            try {
                // 调用图片生成服务
                String originalImageUrl = imageGenerationService.generateWithQwen(task.prompt);

                // 下载图片并上传到MinIO
                byte[] imageBytes = downloadImageFromUrl(originalImageUrl);

                if (imageBytes != null && imageBytes.length > 0) {
                    // 异步线程上 ThreadLocal 丢失，手动设入 userId 供 FeignInterceptor 透传
                    UserContextHolder.setUserId(userId);
                    String fileName = System.currentTimeMillis() + ".png";
                    String ossUrl = ossRpcService.uploadBytes(imageBytes, fileName);

                    // 更新schema中的url为OSS地址
                    task.fieldData.setUrl(ossUrl);
                    if (sink instanceof RecordingSink recording && recording.isEnded()) return;
                    SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "✅ 图片生成完成 (" + current + "/" + total + ")\n");
                    log.info("图片已通过RPC上传到OSS: {} -> {}", task.key, ossUrl);
                } else {
                    throw new RuntimeException("图片下载失败");
                }

            } catch (Exception e) {
                log.error("图片生成或上传失败: {}", task.prompt, e);
                if (sink instanceof RecordingSink recording && recording.isEnded()) return;
                SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "⚠ 图片生成失败 (" + current + "/" + total + "): \n" + task.key);
                // 使用空字符串
                task.fieldData.setUrl("");
            }
        }

        if (sink instanceof RecordingSink recording && recording.isEnded()) return;

        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "✅ 所有图片生成完成\n");
        if (sink instanceof RecordingSink recording && recording.isEnded()) return;
        SseUtil.sendEvent(sink, EventType.TYPE_THINKING, "✅素材准备就绪，开始渲染PPT\n");
    }

    /**
     * 下载图片
     */
    private byte[] downloadImageFromUrl(String imageUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("下载图片失败，状态码: " + response.statusCode());
        }
    }

    @Override
    public PptInstanceStatus getTargetStatus() {
        return TARGET_STATUS;
    }

    /**
     * 图片生成任务
     */
    private static class ImageGenerationTask {
        String key;
        FieldData fieldData;
        String prompt;
        Slide slide;

        ImageGenerationTask(String key, FieldData fieldData, String prompt, Slide slide) {
            this.key = key;
            this.fieldData = fieldData;
            this.prompt = prompt;
            this.slide = slide;
        }
    }
}
    
