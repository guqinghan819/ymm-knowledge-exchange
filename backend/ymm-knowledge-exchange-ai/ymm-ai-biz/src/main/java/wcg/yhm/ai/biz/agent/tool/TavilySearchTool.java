package wcg.yhm.ai.biz.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Tavily 网络搜索工具
 * 
 * 使用步骤：
 * 1. 访问 https://tavily.com/ 注册账号
 * 2. 在 Dashboard 获取 API Key
 * 3. 在 application.yml 中配置: langchain4j.tavily.api-key=your-api-key
 * 
 * 免费额度：每月 1000 次搜索请求
 */
@Component
@Slf4j
public class TavilySearchTool {

    private final WebSearchEngine searchEngine;

    public TavilySearchTool(@Value("${tavily.api.key:}") String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Tavily API Key 未配置，网络搜索功能将不可用");
            this.searchEngine = null;
        } else {
            this.searchEngine = TavilyWebSearchEngine.builder()
                    .apiKey(apiKey)
                    .includeAnswer(true)  // 包含摘要答案
                    .includeRawContent(false)  // 不包含原始内容（节省 Token）
                    .timeout(Duration.ofSeconds(30))  // 超时时间
                    .build();
            log.info("Tavily 搜索引擎初始化成功");
        }
    }

    /**
     * 网络搜索工具方法
     * 
     * @param query 搜索查询关键词
     * @return 搜索结果文本
     */
    @Tool("在互联网上搜索实时信息。当用户询问新闻、最新数据、事实验证或其他需要实时信息的问题时使用此工具。")
    public String searchWeb(String query) {
        if (searchEngine == null) {
            log.warn("搜索引擎未初始化，跳过搜索");
            return "搜索服务未配置，请直接告知用户网络搜索功能暂不可用";
        }

        log.info("执行网络搜索: {}", query);

        try {
            var results = searchEngine.search(query);

            StringBuilder sb = new StringBuilder();
            sb.append("【网络搜索结果】\n\n");

            // 添加详细结果
            if (results.results() != null && !results.results().isEmpty()) {
                sb.append("🔍 详细信息：\n");
                for (int i = 1; i < results.results().size(); i++) { //跳过Tavily Search API
                    var result = results.results().get(i);
                    sb.append(String.format("%d. ", i));
                    sb.append(result.title()).append("\n");
                    sb.append("   链接：").append(result.url()).append("\n");
                    sb.append("   摘要：").append(result.snippet()).append("\n\n");
                }
            } else {
                sb.append("未找到相关搜索结果\n");
            }

            int resultCount = results.results() != null ? results.results().size() : 0;
            log.info("网络搜索完成，返回 {} 条结果", resultCount);
            log.info(" 结果: " + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            log.error("网络搜索失败: {}", query, e);
            return "搜索请求失败，请直接告知用户网络搜索暂时不可用并建议稍后重试";
        }
    }
}
