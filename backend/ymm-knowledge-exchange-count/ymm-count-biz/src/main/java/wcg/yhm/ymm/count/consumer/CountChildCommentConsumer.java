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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.dto.CountCommentMqDTO;
import wcg.yhm.ymm.count.enums.CommentLevelEnum;
import wcg.yhm.ymm.count.mapper.CommentMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: 子评论计数消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_child_comment_total" + MQConstants.TOPIC_COMMENT_COUNT, // Group 组
        topic = MQConstants.TOPIC_COMMENT_COUNT // 主题 Topic
)
@Slf4j
public class CountChildCommentConsumer implements RocketMQListener<String> {
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1)) // 多久聚合一次（1s 一次）
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【笔记二级评论数】聚合消息, size: {}", bodys.size());
        log.info("==> 【笔记二级评论数】聚合消息, {}", JsonUtil.toJsonString(bodys));
        // 将聚合后的消息体 Json 转 List<CountCommentMqDTO>
        List<CountCommentMqDTO> countPublishCommentMqDTOList = new ArrayList<>();
        bodys.forEach(body -> {
            try {
                List<CountCommentMqDTO> list = JsonUtil.str2List(body, CountCommentMqDTO.class);
                countPublishCommentMqDTOList.addAll(list);
            } catch (Exception e) {
                log.error("", e);
            }
        });
        // 过滤出二级评论，并按 parent_id 分组
        Map<Long, List<CountCommentMqDTO>> groupMap = countPublishCommentMqDTOList.stream()
                .filter(commentMqDTO -> Objects.equals(CommentLevelEnum.TWO.getCode(), commentMqDTO.getLevel()))
                .collect(Collectors.groupingBy(CountCommentMqDTO::getParentId)); // 按 parent_id 分组
        // 若无二级评论，则直接 return
        if (CollUtil.isEmpty(groupMap)) return;
        // 循环分组字典
        for (Map.Entry<Long, List<CountCommentMqDTO>> entry : groupMap.entrySet()) {
            // 一级评论 ID
            Long parentId = entry.getKey();
            // 评论数
            int count = CollUtil.size(entry.getValue());

            // 更新一级评论的下级评论总数，进行累加操作
            commentMapper.updateChildCommentTotal(parentId, count);
        }

        // 获取字典中所有评论 ID
        Set<Long> commentIds = groupMap.keySet();

        // 异步发送计数 MQ, 更新评论热度值
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(commentIds))
                .build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_HEAT_UPDATE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论热度值更新】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【评论热度值更新】MQ 发送异常: ", throwable);
            }
        });
    }
}
