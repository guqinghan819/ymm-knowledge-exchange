# LangChain4j + Tavily 网络搜索使用教程

## 📋 目录
1. [什么是 Tavily](#什么是-tavily)
2. [快速开始](#快速开始)
3. [配置说明](#配置说明)
4. [使用示例](#使用示例)
5. [高级用法](#高级用法)
6. [常见问题](#常见问题)

---

## 什么是 Tavily

Tavily 是一个专为 AI 应用设计的搜索引擎 API，具有以下特点：

✅ **为 LLM 优化**：返回的内容更适合大语言模型处理  
✅ **实时信息**：获取最新的新闻、数据和技术动态  
✅ **免费额度**：每月 1000 次免费搜索请求  
✅ **简单易用**：LangChain4j 官方支持，集成简单  

官方网站：https://tavily.com/

---

## 快速开始

### 步骤 1：获取 API Key

1. 访问 https://tavily.com/
2. 点击 "Sign Up" 注册账号
3. 登录后进入 Dashboard
4. 复制您的 API Key

### 步骤 2：添加 Maven 依赖

在 `pom.xml` 中添加（项目已添加）：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-web-search-engine-tavily</artifactId>
</dependency>
```

### 步骤 3：配置 API Key

在 `application.yml` 中配置：

```yaml
langchain4j:
  tavily:
    api-key: your-tavily-api-key-here  # 替换为您的真实 API Key
```

### 步骤 4：创建搜索工具类

参考 `TavilySearchTool.java`：

```java
@Component
public class TavilySearchTool {
    
    private final WebSearchEngine searchEngine;

    public TavilySearchTool(@Value("${langchain4j.tavily.api-key}") String apiKey) {
        this.searchEngine = TavilyWebSearchEngine.builder()
                .apiKey(apiKey)
                .maxResults(5)
                .includeAnswer(true)
                .includeRawContent(false)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    @Tool("在互联网上搜索实时信息")
    public String searchWeb(String query) {
        var results = searchEngine.search(query);
        // 处理结果...
        return formattedResults;
    }
}
```

### 步骤 5：注册到 AI Service

在配置类中：

```java
@Bean
public WebSearchAiService webSearchAiService(TavilySearchTool searchTool) {
    return AiServices.builder(WebSearchAiService.class)
            .streamingChatModel(streamingChatModel)
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
            .tools(searchTool)  // ← 关键：注册工具
            .build();
}
```

---

## 配置说明

### TavilyWebSearchEngine 参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `apiKey` | String | 必填 | Tavily API Key |
| `maxResults` | int | 5 | 每次搜索返回的结果数量（1-10） |
| `includeAnswer` | boolean | false | 是否包含摘要答案 |
| `includeRawContent` | boolean | false | 是否包含原始网页内容（会增加 Token 消耗） |
| `timeout` | Duration | 30s | 请求超时时间 |

### 推荐配置

```java
TavilyWebSearchEngine.builder()
    .apiKey(apiKey)
    .maxResults(5)              // 5 条结果足够，平衡质量和成本
    .includeAnswer(true)        // 开启摘要，AI 回答更准确
    .includeRawContent(false)   // 关闭原始内容，节省 Token
    .timeout(Duration.ofSeconds(30))
    .build();
```

---

## 使用示例

### 示例 1：基本对话（不需要搜索）

```bash
curl "http://localhost:8098/agent/chat/stream?query=你好&conversationId=test123"
```

**AI 行为**：直接回答，不调用搜索工具

### 示例 2：需要实时信息的问题

```bash
curl "http://localhost:8098/agent/chat/stream?query=今天有什么科技新闻&conversationId=test123"
```

**AI 行为**：
1. 识别到需要实时信息
2. 自动调用 `searchWeb()` 工具
3. 基于搜索结果生成回答

### 示例 3：事实验证

```bash
curl "http://localhost:8098/agent/chat/stream?query=Python最新版本是多少&conversationId=test123"
```

**AI 行为**：调用搜索工具获取最新信息

### 示例 4：多轮对话（保持上下文）

```bash
# 第一轮
curl "http://localhost:8098/agent/chat/stream?query=最近有什么AI技术突破&conversationId=session1"

# 第二轮（AI 记得之前的对话）
curl "http://localhost:8098/agent/chat/stream?query=详细说说第二个突破&conversationId=session1"
```

---

## 高级用法

### 1. 自定义搜索结果处理

```java
@Tool("搜索网络信息")
public String searchWeb(String query) {
    var results = searchEngine.search(query);
    
    StringBuilder sb = new StringBuilder();
    
    // 自定义格式
    if (results.answer() != null) {
        sb.append("摘要：").append(results.answer()).append("\n\n");
    }
    
    for (var result : results.results()) {
        sb.append("标题：").append(result.title()).append("\n");
        sb.append("来源：").append(result.url()).append("\n");
        sb.append("内容：").append(result.snippet()).append("\n\n");
    }
    
    return sb.toString();
}
```

### 2. 条件性启用搜索

在 System Message 中指导 AI：

```java
@SystemMessage(value = """
    你是智能助手。
    
    ## 何时使用搜索工具
    - 用户询问新闻、时事、最新数据
    - 需要验证实时信息
    - 查询最新的技术文档或 API
    
    ## 何时不使用搜索工具
    - 通用知识问答
    - 代码编写和调试
    - 逻辑推理和计算
    """)
Flux<String> chat(@MemoryId String conversationId, @UserMessage String query);
```

### 3. 多个工具组合

```java
@Bean
public WebSearchAiService webSearchAiService(
        TavilySearchTool searchTool,
        CalculatorTool calculatorTool,
        WeatherTool weatherTool) {
    return AiServices.builder(WebSearchAiService.class)
            .streamingChatModel(streamingChatModel)
            .tools(searchTool, calculatorTool, weatherTool)  // 注册多个工具
            .build();
}
```

---

## 常见问题

### Q1: 如何知道 AI 是否调用了搜索工具？

**A**: 查看日志，会输出：
```
执行网络搜索: 今天有什么科技新闻
网络搜索完成，返回 5 条结果
```

### Q2: 搜索失败怎么办？

**A**: 
1. 检查 API Key 是否正确
2. 确认网络连接正常
3. 查看错误日志
4. 确认未超出免费额度（每月 1000 次）

### Q3: 如何控制搜索成本？

**A**:
- 减少 `maxResults` 数量（如设置为 3）
- 关闭 `includeRawContent`
- 在 System Message 中明确何时使用搜索

### Q4: 搜索结果不准确？

**A**:
- 优化用户的查询关键词
- 增加 `maxResults` 获取更多结果
- 开启 `includeAnswer` 获取摘要

### Q5: 支持中文搜索吗？

**A**: ✅ 完全支持中文搜索和中文结果

### Q6: 免费额度用完了怎么办？

**A**: 
- 升级付费套餐（$29/月起）
- 或者切换到其他搜索引擎（如 Serper、Google Custom Search）

---

## 最佳实践

### ✅ 推荐做法

1. **明确工具用途**：在 `@Tool` 注解中清晰描述何时使用该工具
2. **合理设置 maxResults**：一般 3-5 条足够
3. **开启 includeAnswer**：提高回答质量
4. **记录搜索日志**：便于调试和分析
5. **优雅处理错误**：避免搜索失败导致整个请求失败

### ❌ 避免的做法

1. ~~每次请求都强制搜索~~（让 AI 自主判断）
2. ~~设置过大的 maxResults~~（浪费 Token 和成本）
3. ~~开启 includeRawContent~~（除非确实需要）
4. ~~忽略错误处理~~（会导致用户体验差）

---

## 完整工作流程

```
用户提问
  ↓
AI 分析问题
  ↓
需要实时信息？
  ├─ 是 → 调用 Tavily 搜索工具
  │        ↓
  │     获取搜索结果
  │        ↓
  │     基于结果生成回答
  │
  └─ 否 → 直接生成回答
           ↓
      流式返回给用户
```

---

## 参考资料

- LangChain4j 官方文档：https://docs.langchain4j.dev/
- Tavily 官方文档：https://docs.tavily.com/
- Tavily 定价：https://tavily.com/#pricing

---

**祝您使用愉快！** 🎉
