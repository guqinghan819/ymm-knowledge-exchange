package wcg.yhm.ai.biz.agent.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.domain.pojo.AiPptTemplate;
import wcg.yhm.ai.biz.rpc.OssRpcService;
import wcg.yhm.ai.biz.service.IAiPptTemplateService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * PPT Python 渲染服务实现
 */
@Slf4j
@Service
public class PptPythonRenderServiceImpl implements PptPythonRenderService {

    @Resource
    private IAiPptTemplateService aiPptTemplateService;

    @Resource
    private OssRpcService ossRpcService;

    @Value("${ppt.render.python-command:python3}")
    private String pythonCommand;

    @Override
    public String renderPpt(AiPptInst inst, String pptSchema) throws Exception {

        log.info("开始渲染PPT: instId={}", inst.getId());

        List<Path> temporaryFiles = new ArrayList<>();
        Path outputFilePath = null;

        try {
            // ---------- 获取模板 ----------
            AiPptTemplate template = aiPptTemplateService.getByCode(inst.getTemplateCode());
            if (template == null) {
                throw new RuntimeException("模板不存在: " + inst.getTemplateCode());
            }

            Path pythonScriptPath = materializeClasspathResource("python/render_ppt.py", ".py");
            temporaryFiles.add(pythonScriptPath);
            Path templateFilePath = resolveTemplateFile(template, temporaryFiles);
            String outputDir = getOutputDir();

            String outputFileName = "ppt_" + inst.getId() + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".pptx";

            outputFilePath = Paths.get(outputDir, outputFileName);

            // ---------- 构建命令 ----------
            List<String> command = List.of(
                    pythonCommand,
                    pythonScriptPath.toString(),
                    "--template", templateFilePath.toString(),
                    "--output", outputFilePath.toString()
            );

            log.info("执行Python命令: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");

            // ---------- 处理 JSON 传递 ----------
            // Windows 环境变量长度有限（32KB），大 JSON 自动写入临时文件
            if (pptSchema.length() > 20000) {
                Path tempFile = Files.createTempFile("ppt_schema_", ".json");
                temporaryFiles.add(tempFile);
                Files.writeString(tempFile, pptSchema, StandardOpenOption.TRUNCATE_EXISTING);
                env.put("PPT_SCHEMA_FILE", tempFile.toAbsolutePath().toString());
                log.info("JSON过大，使用临时文件传递: {}", tempFile);
            } else {
                env.put("PPT_SCHEMA", pptSchema);
            }

            // ---------- 启动 ----------
            Process process = pb.start();

            // ---------- 异步读取输出（避免管道死锁） ----------
            StringBuilder output = new StringBuilder();
            Thread outputReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        log.info("Python输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取Python输出异常", e);
                }
            }, "python-output-reader");
            outputReader.start();

            // ---------- 等待（最多5分钟） ----------
            long timeoutMs = 5 * 60 * 1000L;
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                outputReader.interrupt();
                throw new RuntimeException("Python执行超时");
            }

            // 等待输出线程读完最后的内容
            outputReader.join(5000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Python执行失败: {}", output);
                throw new RuntimeException("Python脚本执行失败:\n" + output);
            }

            // ---------- 检查输出 ----------
            if (!Files.exists(outputFilePath)) {
                throw new RuntimeException("PPT未生成: " + outputFilePath);
            }

            // ---------- 上传到OSS（RPC方式，路径由MinioOssStrategy统一生成） ----------
            log.info("PPT生成成功，开始上传到OSS");
            byte[] fileBytes = Files.readAllBytes(outputFilePath);
            String fileUrl = ossRpcService.uploadPptx(fileBytes, outputFileName);

            log.info("PPT已通过RPC上传到OSS: {}", fileUrl);
            return fileUrl;
        } finally {
            deleteIfExists(outputFilePath, "本地PPT文件");
            for (Path temporaryFile : temporaryFiles) {
                deleteIfExists(temporaryFile, "PPT临时文件");
            }
        }
    }

    private Path resolveTemplateFile(AiPptTemplate template, List<Path> temporaryFiles) throws IOException {
        String configuredPath = template.getFilePath();
        if (configuredPath != null && configuredPath.startsWith("classpath:")) {
            Path materialized = materializeClasspathResource(configuredPath.substring("classpath:".length()), ".pptx");
            temporaryFiles.add(materialized);
            return materialized;
        }

        if (configuredPath != null && !configuredPath.isBlank()) {
            Path externalPath = Paths.get(configuredPath);
            if (Files.isRegularFile(externalPath)) {
                return externalPath;
            }
            log.warn("配置的PPT模板文件不存在: {}，尝试从classpath加载", configuredPath);
        }

        Path materialized = materializeClasspathResource("templates/" + template.getTemplateCode() + ".pptx", ".pptx");
        temporaryFiles.add(materialized);
        return materialized;
    }

    private Path materializeClasspathResource(String location, String suffix) throws IOException {
        String normalizedLocation = location.startsWith("/") ? location.substring(1) : location;
        ClassPathResource resource = new ClassPathResource(
                normalizedLocation, PptPythonRenderServiceImpl.class.getClassLoader());
        if (!resource.exists()) {
            throw new FileNotFoundException("classpath资源不存在: " + normalizedLocation);
        }

        Path tempFile = Files.createTempFile("ppt_resource_", suffix);
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private void deleteIfExists(Path path, String description) {
        if (path == null) {
            return;
        }
        try {
            if (Files.deleteIfExists(path)) {
                log.info("{}已删除: {}", description, path);
            }
        } catch (Exception e) {
            log.warn("删除{}失败: {}", description, path, e);
        }
    }

    /**
     * 获取输出目录（用于临时存储）
     */
    private String getOutputDir() {
        String projectRoot = System.getProperty("user.dir");
        String outputDir = projectRoot + File.separator + "output" + File.separator + "ppt";
        try {
            Path path = Paths.get(outputDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            log.error("创建输出目录失败: {}", outputDir, e);
        }
        return outputDir;
    }
}
