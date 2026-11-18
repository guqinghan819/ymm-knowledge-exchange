package wcg.yhm.ymm.count.consumer;

import cn.hutool.core.collection.CollUtil;
import com.github.phantomthief.collection.BufferTrigger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.dto.AggregationCommentLikeDTO;
import wcg.yhm.ymm.count.domain.dto.CountCommentLikeUnlikeDTO;
import wcg.yhm.ymm.count.enums.CommentLikeEnum;
import wcg.yhm.ymm.count.mapper.CommentMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论点赞、取消点赞计数消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_count_" + MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE,
        topic = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE)
public class CountCommentLikeConsumer implements RocketMQListener<String> {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private final BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000)
            .batchSize(1000)
            .linger(Duration.ofSeconds(1))
            .setConsumerEx(this::consumeMessage)
            .build();

    @Override
    public void onMessage(String body) {
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【评论点赞计数】聚合消息, size: {}", bodys.size());
        log.info("==> 【评论点赞计数】聚合消息, {}", JsonUtil.toJsonString(bodys));

        List<CountCommentLikeUnlikeDTO> operations = new ArrayList<>();
        bodys.forEach(body -> {
            try {
                CountCommentLikeUnlikeDTO dto = JsonUtil.parseObject(body, CountCommentLikeUnlikeDTO.class);
                if (Objects.nonNull(dto)
                        && Objects.nonNull(dto.getCommentId())
                        && Objects.nonNull(dto.getType())) {
                    operations.add(dto);
                }
            } catch (Exception e) {
                log.error("==> 评论点赞计数消息解析失败", e);
            }
        });
        if (CollUtil.isEmpty(operations)) {
            return;
        }

        Map<Long, List<CountCommentLikeUnlikeDTO>> groupMap = operations.stream()
                .collect(Collectors.groupingBy(CountCommentLikeUnlikeDTO::getCommentId));
        List<AggregationCommentLikeDTO> countList = new ArrayList<>();
        for (Map.Entry<Long, List<CountCommentLikeUnlikeDTO>> entry : groupMap.entrySet()) {
            long count = 0L;
            for (CountCommentLikeUnlikeDTO dto : entry.getValue()) {
                CommentLikeEnum likeEnum = CommentLikeEnum.getEnum(dto.getType());
                if (Objects.isNull(likeEnum)) {
                    continue;
                }
                switch (likeEnum) {
                    case LIKE -> count += 1;
                    case UNLIKE -> count -= 1;
                }
            }
            if (count != 0L) {
                countList.add(new AggregationCommentLikeDTO(entry.getKey(), count));
            }
        }
        if (CollUtil.isEmpty(countList)) {
            return;
        }

        int rows = commentMapper.batchUpdateCommentLikeTotal(countList);
        if (rows <= 0) {
            return;
        }

        Set<Long> commentIds = countList.stream()
                .map(AggregationCommentLikeDTO::getCommentId)
                .collect(Collectors.toSet());
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(commentIds)).build();
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_HEAT_UPDATE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论点赞热度更新】MQ 发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【评论点赞热度更新】MQ 发送失败", throwable);
            }
        });
    }
}
