package wcg.yhm.ai.biz.controller;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import wcg.yhm.ai.biz.domain.request.AiWriteContentRequest;
import wcg.yhm.ai.biz.domain.request.AiWriteTitleRequest;
import wcg.yhm.ai.biz.domain.request.ImageWritingRequest;
import wcg.yhm.ai.biz.domain.request.PptCreateRequest;
import wcg.yhm.ai.biz.domain.request.PublishAssistantRequest;
import wcg.yhm.ai.biz.domain.request.WebSearchRequest;
import wcg.yhm.ai.biz.manager.TaskManager;
import wcg.yhm.ai.biz.service.AiService;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;


/**
 * Agent 控制器 - 支持 Tavily 网络搜索
 *
 * 使用示例：
 * 1. 普通对话：http://localhost:8098/agent/chat/stream?query=你好&conversationId=test123
 * 2. 需要搜索的问题：http://localhost:8098/agent/chat/stream?query=今天有什么科技新闻&conversationId=test123
 */
@RestController
@RequestMapping("/agent")
@Slf4j
public class AgentController {

    @Resource
    private AiService aiService;
    @Resource
    private TaskManager taskManager;

    /**
     * 流式聊天接口（支持 Tavily 网络搜索）
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chatStream(@RequestBody @Validated WebSearchRequest webSearchRequest,
                                   HttpServletResponse httpServletResponse) {
        String query = webSearchRequest.getQuery();
        String conversationId = webSearchRequest.getConversationId();
        httpServletResponse.setCharacterEncoding("UTF-8");
        return aiService.webSearch(conversationId, query);
    }

    @PostMapping(value = "/ppt/create/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> pptCreate(@RequestBody @Validated PptCreateRequest pptCreateRequest,
                                  HttpServletResponse httpServletResponse) {
        String query = pptCreateRequest.getQuery();
        String conversationId = pptCreateRequest.getConversationId();
        httpServletResponse.setCharacterEncoding("UTF-8");
        return aiService.pptCreate(conversationId, query);
    }

    @PostMapping(value = "/note/title/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> noteTitle(@RequestBody @Validated AiWriteTitleRequest request,
                                  HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");
        return aiService.writeNoteTitle(request);
    }

    @PostMapping(value = "/note/content/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> noteContent(@RequestBody @Validated AiWriteContentRequest request,
                                    HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");
        return aiService.writeNoteContent(request);
    }

    @PostMapping(value = "/image-writing/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> imageWriting(@RequestBody @Validated ImageWritingRequest request,
                                     HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");
        return aiService.imageWriting(request);
    }

    @PostMapping(value = "/publish-assistant/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> publishAssistant(@RequestBody @Validated PublishAssistantRequest request,
                                         HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");
        return aiService.publishAssistant(request);
    }

    /**
     * 停止对话
     */
    @GetMapping("/chat/stop")
    public Response stop(@RequestParam(required = true) String conversationId) {
        Long userId = UserContextHolder.getUserId();
        String taskKey = userId + ":" + conversationId;
        log.info("停止请求: taskKey={}", taskKey);
        boolean success = taskManager.cancel(taskKey);
        if (success) {
            return Response.success();
        }
        return Response.fail();
    }




}
