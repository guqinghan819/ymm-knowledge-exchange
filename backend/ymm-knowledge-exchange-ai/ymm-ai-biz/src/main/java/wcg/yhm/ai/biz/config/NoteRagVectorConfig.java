package wcg.yhm.ai.biz.config;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

/**
 * 笔记 RAG 向量化配置。
 */
@Configuration
public class NoteRagVectorConfig {

    @Value("${langchain4j.dashscope.api-key}")
    private String apiKey;

    @Value("${langchain4j.dashscope.base-url}")
    private String baseUrl;

    @Value("${langchain4j.dashscope.embedding-model-name:text-embedding-v4}")
    private String embeddingModelName;

    @Value("${langchain4j.dashscope.embedding-dimension:1024}")
    private Integer embeddingDimension;

    @Value("${rag.note.vector.qdrant.host:127.0.0.1}")
    private String qdrantHost;

    @Value("${rag.note.vector.qdrant.port:6334}")
    private Integer qdrantPort;

    @Value("${rag.note.vector.qdrant.use-tls:false}")
    private Boolean qdrantUseTls;

    @Value("${rag.note.vector.qdrant.api-key:}")
    private String qdrantApiKey;

    @Value("${rag.note.vector.qdrant.collection-name:note_rag_child_chunk_v2}")
    private String collectionName;

    @Value("${rag.note.vector.qdrant.dense-vector-name:dense}")
    private String denseVectorName;

    @Value("${rag.note.vector.qdrant.sparse-vector-name:sparse}")
    private String sparseVectorName;

    @Bean
    public EmbeddingModel noteRagEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .dimensions(embeddingDimension)
                .encodingFormat("float")
                .timeout(Duration.ofMinutes(2))
                .maxRetries(2)
                .build();
    }

    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, qdrantUseTls);
        if (StrUtil.isNotBlank(qdrantApiKey)) {
            builder.withApiKey(qdrantApiKey);
        }
        QdrantClient client = new QdrantClient(builder.withTimeout(Duration.ofSeconds(10)).build());
        ensureCollection(client);
        return client;
    }

    private void ensureCollection(QdrantClient qdrantClient) {
        try {
            Boolean exists = qdrantClient.collectionExistsAsync(collectionName, Duration.ofSeconds(10)).get();
            if (Boolean.TRUE.equals(exists)) {
                return;
            }

            Collections.VectorParams denseParams = Collections.VectorParams.newBuilder()
                    .setSize(embeddingDimension)
                    .setDistance(Collections.Distance.Cosine)
                    .build();
            Collections.VectorParamsMap vectorParamsMap = Collections.VectorParamsMap.newBuilder()
                    .putMap(denseVectorName, denseParams)
                    .build();
            Collections.SparseVectorParams sparseParams = Collections.SparseVectorParams.newBuilder()
                    .setModifier(Collections.Modifier.Idf)
                    .build();
            Collections.CreateCollection request = Collections.CreateCollection.newBuilder()
                    .setCollectionName(collectionName)
                    .setVectorsConfig(Collections.VectorsConfig.newBuilder().setParamsMap(vectorParamsMap))
                    .setSparseVectorsConfig(Collections.SparseVectorConfig.newBuilder()
                            .putMap(sparseVectorName, sparseParams))
                    .build();
            qdrantClient.createCollectionAsync(request, Duration.ofSeconds(30)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("初始化Qdrant集合被中断: collectionName=" + collectionName, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("初始化Qdrant集合失败: collectionName=" + collectionName, e);
        }
    }
}
