package wcg.yhm.ymm.count.consumer;

import cn.hutool.core.collection.CollUtil;
import com.github.phantomthief.collection.BufferTrigger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.dto.CountCommentMqDTO;
import wcg.yhm.ymm.count.mapper.NoteCountMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: 笔记评论计数消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_COMMENT_COUNT, // Group 组
        topic = MQConstants.TOPIC_COMMENT_COUNT // 主题 Topic
)
@Slf4j
public class CountNoteCommentConsumer implements RocketMQListener<String> {
    @Resource
    private NoteCountMapper noteCountMapper;

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
        log.info("==> 【笔记评论数】聚合消息, size: {}", bodys.size());
        log.info("==> 【笔记评论数】聚合消息, {}", JsonUtil.toJsonString(bodys));
        // 将聚合后的消息体 Json 转 List<CountCommentMqDTO>
        List<CountCommentMqDTO> countCommentMqDTOList = new ArrayList<>();
        bodys.forEach(body -> {
            try {
                List<CountCommentMqDTO> list = JsonUtil.str2List(body, CountCommentMqDTO.class);
                countCommentMqDTOList.addAll(list);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        // 按笔记 ID 进行分组
        Map<Long, List<CountCommentMqDTO>> groupMap = countCommentMqDTOList.stream()
                .collect(Collectors.groupingBy(CountCommentMqDTO::getNoteId));

        // 循环分组字典
        for (Map.Entry<Long, List<CountCommentMqDTO>> entry : groupMap.entrySet()) {
            // 笔记 ID
            Long noteId = entry.getKey();
            // 评论数
            int count = CollUtil.size(entry.getValue());

            // 若评论数大于零，则执行更新操作：累加评论总数
            if (count > 0) {
                noteCountMapper.insertOrUpdateCommentTotalByNoteId(count, noteId);
            }
        }
    }
}

    
