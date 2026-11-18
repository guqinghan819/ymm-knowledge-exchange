package wcg.yhm.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import wcg.yhm.comment.biz.constants.MQConstants;
import wcg.yhm.comment.biz.domain.dto.CommentLikeUnlikeMqDTO;
import wcg.yhm.comment.biz.domain.po.CommentLike;
import wcg.yhm.comment.biz.mapper.CommentLikeMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论点赞、取消点赞状态落库消费者
 */
@Component
@Slf4j
public class CommentLikeUnlikeConsumer {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    @Resource
    private CommentLikeMapper commentLikeMapper;

    private DefaultMQPushConsumer consumer;

    private final RateLimiter rateLimiter = RateLimiter.create(5000);

    @Bean(name = "CommentLikeUnlikeConsumer")
    public DefaultMQPushConsumer commentLikeUnlikeConsumer() throws MQClientException {
        String group = "ymm_group_status_" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE;
        consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(nameServerAddr);
        consumer.subscribe(MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE, "*");
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setMaxReconsumeTimes(3);
        consumer.setConsumeMessageBatchMaxSize(30);
        consumer.setPullInterval(1000);

        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            log.info("==> 【评论点赞、取消点赞状态】本批次消息大小: {}", msgs.size());
            try {
                rateLimiter.acquire();
                List<CommentLikeUnlikeMqDTO> operations = parseMessages(msgs);
                List<CommentLikeUnlikeMqDTO> finalOperations = mergeLatestOperation(operations);
                if (CollUtil.isEmpty(finalOperations)) {
                    return ConsumeOrderlyStatus.SUCCESS;
                }

                List<CommentLike> commentLikes = finalOperations.stream()
                        .map(dto -> new CommentLike()
                                .setUserId(dto.getUserId())
                                .setCommentId(dto.getCommentId())
                                .setStatus(dto.getType())
                                .setCreateTime(dto.getCreateTime()))
                        .toList();
                commentLikeMapper.batchInsertOrUpdate(commentLikes);
                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                log.error("==> 评论点赞状态落库失败", e);
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });

        consumer.start();
        return consumer;
    }

    private List<CommentLikeUnlikeMqDTO> parseMessages(List<MessageExt> msgs) {
        List<CommentLikeUnlikeMqDTO> operations = new ArrayList<>();
        msgs.forEach(msg -> {
            try {
                String msgJson = new String(msg.getBody());
                log.info("==> 评论点赞状态 Consumer - Received message: {}", msgJson);
                CommentLikeUnlikeMqDTO dto = JsonUtil.parseObject(msgJson, CommentLikeUnlikeMqDTO.class);
                if (Objects.nonNull(dto)
                        && Objects.nonNull(dto.getUserId())
                        && Objects.nonNull(dto.getCommentId())
                        && Objects.nonNull(dto.getType())) {
                    operations.add(dto);
                }
            } catch (Exception e) {
                log.error("==> 评论点赞状态消息解析失败", e);
            }
        });
        return operations;
    }

    private List<CommentLikeUnlikeMqDTO> mergeLatestOperation(List<CommentLikeUnlikeMqDTO> operations) {
        if (CollUtil.isEmpty(operations)) {
            return List.of();
        }
        Map<String, CommentLikeUnlikeMqDTO> operationMap = operations.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getUserId() + ":" + dto.getCommentId(),
                        dto -> dto,
                        (first, second) -> second,
                        LinkedHashMap::new));
        return new ArrayList<>(operationMap.values());
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(consumer)) {
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.error("==> 关闭评论点赞状态消费者失败", e);
            }
        }
    }
}
