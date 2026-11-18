package wcg.yhm.ai.biz.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import wcg.yhm.ai.biz.agent.tool.TitleGenerator;
import wcg.yhm.ai.biz.domain.pojo.AiConversation;
import wcg.yhm.ai.biz.domain.pojo.AiMessage;
import wcg.yhm.ai.biz.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AiConversationRecordService {
    @Resource private IAiConversationService conversations;
    @Resource private IAiMessageService messages;
    @Resource private TransactionTemplate transactionTemplate;
    @Resource private TitleGenerator titleGenerator;
    @Resource(name = "taskExecutor") private ThreadPoolTaskExecutor executor;

    public RecordingSink begin(Long userId, String conversationId, String query, String agentType,
                               Map<String, Object> metadata) {
        if (userId == null || StrUtil.isBlank(conversationId) || StrUtil.isBlank(query)) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        String defaultTitle = StrUtil.sub(query.trim().replaceAll("\\s+", " "), 0, 40);
        boolean[] created = {false};
        AiMessage message = transactionTemplate.execute(tx -> {
            AiConversation conversation = conversations.lambdaQuery().eq(AiConversation::getUserId, userId)
                    .eq(AiConversation::getConversationId, conversationId).one();
            if (conversation == null) {
                try {
                    requireSaved(conversations.save(AiConversation.builder().userId(userId)
                            .conversationId(conversationId).title(defaultTitle).agentType(agentType)
                            .createdTime(LocalDateTime.now()).updatedTime(LocalDateTime.now()).deleted(0).build()));
                    created[0] = true;
                } catch (DuplicateKeyException e) {
                    conversation = conversations.lambdaQuery().eq(AiConversation::getUserId, userId)
                            .eq(AiConversation::getConversationId, conversationId).one();
                    if (conversation == null) throw e;
                }
            }
            if (conversation != null && Integer.valueOf(1).equals(conversation.getDeleted())) {
                throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
            }
            requireSaved(conversations.lambdaUpdate().eq(AiConversation::getUserId, userId)
                    .eq(AiConversation::getConversationId, conversationId).eq(AiConversation::getDeleted, 0)
                    .set(AiConversation::getAgentType, agentType).set(AiConversation::getUpdatedTime, LocalDateTime.now()).update());
            AiMessage row = AiMessage.builder().messageId(UUID.randomUUID().toString()).userId(userId)
                    .conversationId(conversationId).question(query).agentType(agentType).status("GENERATING")
                    .metadata(JSON.toJSONString(metadata)).deleted(0)
                    .createdTime(LocalDateTime.now()).updatedTime(LocalDateTime.now()).build();
            requireSaved(messages.save(row));
            return row;
        });
        return new RecordingSink(message, metadata, snapshot -> {
            finish(snapshot);
            if (created[0]) generateTitle(userId, conversationId, query, defaultTitle);
        });
    }

    void finish(AiMessage message) {
        transactionTemplate.executeWithoutResult(tx -> {
            // A deletion owns the final decision; callbacks never revive deleted records.
            boolean active = conversations.lambdaQuery().eq(AiConversation::getUserId, message.getUserId())
                    .eq(AiConversation::getConversationId, message.getConversationId())
                    .eq(AiConversation::getDeleted, 0).exists();
            if (!active) return;
            boolean updated = messages.lambdaUpdate().eq(AiMessage::getUserId, message.getUserId())
                    .eq(AiMessage::getMessageId, message.getMessageId()).eq(AiMessage::getDeleted, 0)
                    .eq(AiMessage::getStatus, "GENERATING")
                    .set(AiMessage::getAnswer, message.getAnswer()).set(AiMessage::getThinking, message.getThinking())
                    .set(AiMessage::getReference, message.getReference()).set(AiMessage::getRecommend, message.getRecommend())
                    .set(AiMessage::getMetadata, message.getMetadata()).set(AiMessage::getStatus, message.getStatus())
                    .set(AiMessage::getErrorMessage, message.getErrorMessage())
                    .set(AiMessage::getUpdatedTime, LocalDateTime.now()).update();
            if (!updated) {
                boolean deleted = messages.lambdaQuery().eq(AiMessage::getUserId, message.getUserId())
                        .eq(AiMessage::getMessageId, message.getMessageId()).eq(AiMessage::getDeleted, 1).exists();
                if (!deleted) throw new IllegalStateException("对话记录未更新: " + message.getMessageId());
                return;
            }
            conversations.lambdaUpdate().eq(AiConversation::getUserId, message.getUserId())
                    .eq(AiConversation::getConversationId, message.getConversationId()).eq(AiConversation::getDeleted, 0)
                    .set(AiConversation::getUpdatedTime, LocalDateTime.now()).update();
        });
    }

    private void generateTitle(Long userId, String conversationId, String query, String defaultTitle) {
        try {
            executor.execute(() -> {
                try {
                    String title = titleGenerator.generate(query);
                    if (StrUtil.isNotBlank(title)) conversations.lambdaUpdate()
                            .eq(AiConversation::getUserId, userId).eq(AiConversation::getConversationId, conversationId)
                            .eq(AiConversation::getDeleted, 0).eq(AiConversation::getTitle, defaultTitle)
                            .set(AiConversation::getTitle, StrUtil.sub(title, 0, 512)).update();
                } catch (Exception e) { log.warn("会话标题生成失败: {}", conversationId, e); }
            });
        } catch (RuntimeException e) { log.warn("会话标题任务提交失败: {}", conversationId, e); }
    }

    private void requireSaved(boolean saved) {
        if (!saved) throw new IllegalStateException("对话记录保存失败");
    }
}
