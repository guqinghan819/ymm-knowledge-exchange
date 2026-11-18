package wcg.yhm.ai.biz.agent.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-01
 * @Description:
 * @Version: 1.0
 */

@Configuration
public class ChatModelConfig {
    @Value("${langchain4j.dashscope.api-key}")
    private String apikey;

    @Value("${langchain4j.dashscope.base-url}")
    private String baseUrl;

    @Value("${langchain4j.dashscope.chat-model-name}")
    private String chatModelName;

    @Value("${langchain4j.dashscope.vision-model-name}")
    private String visionModelName;

    @Value("${langchain4j.dashscope.moderation-text-model-name}")
    private String moderationTextModelName;

    @Value("${langchain4j.dashscope.moderation-vision-model-name}")
    private String moderationVisionModelName;

    @Value("${langchain4j.dashscope.structured-model-name}")
    private String structuredModelName;

    // Existing workflows consume final text/JSON and have short moderation deadlines.
    private static final Map<String, Object> DIRECT_RESPONSE = Map.of("enable_thinking", false);

    @Bean
    public StreamingChatModel  streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apikey)
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .customParameters(DIRECT_RESPONSE)
                .build();
    }

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apikey)
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .customParameters(DIRECT_RESPONSE)
                .timeout(Duration.ofMinutes(10))
                .build();
    }

    @Bean
    public ChatModel qwenVisionChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apikey)
                .baseUrl(baseUrl)
                .modelName(visionModelName)
                .customParameters(DIRECT_RESPONSE)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    public ChatModel moderationTextModel() {
        return OpenAiChatModel.builder().apiKey(apikey).baseUrl(baseUrl)
                .modelName(moderationTextModelName).customParameters(DIRECT_RESPONSE)
                .timeout(Duration.ofSeconds(35)).build();
    }

    @Bean
    public ChatModel moderationVisionModel() {
        return OpenAiChatModel.builder().apiKey(apikey).baseUrl(baseUrl)
                .modelName(moderationVisionModelName).customParameters(DIRECT_RESPONSE)
                .timeout(Duration.ofSeconds(25)).build();
    }

    /**
     * 意图识别专用 ChatModel，输出 PptIntentResult
     */
    @Bean
    public ChatModel intentChatModel() {
        List<String> enums = List.of("CREATE_PPT", "MODIFY_PPT", "RESUME_PPT");
        return OpenAiChatModel.builder()
                .apiKey(apikey)
                .baseUrl(baseUrl)
                .modelName(structuredModelName)
                .customParameters(DIRECT_RESPONSE)
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(JsonSchema.builder()
                                .name("PptIntentResult")
                                .rootElement(JsonObjectSchema.builder()
                                        .addStringProperty("reason")
                                        .addEnumProperty("pptIntent", enums)
                                        .required("reason", "pptIntent")
                                        .build())
                                .build())
                        .build())
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    /**
     * 模板选择专用 ChatModel，输出 TemplateSelectionResult
     */
    @Bean
    public ChatModel templateChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apikey)
                .baseUrl(baseUrl)
                .modelName(structuredModelName)
                .customParameters(DIRECT_RESPONSE)
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(JsonSchema.builder()
                                .name("TemplateSelectionResult")
                                .rootElement(JsonObjectSchema.builder()
                                        .addStringProperty("templateCode")
                                        .addStringProperty("reason")
                                        .required("templateCode", "reason")
                                        .build())
                                .build())
                        .build())
                .timeout(Duration.ofMinutes(5))
                .build();
    }

}
    
