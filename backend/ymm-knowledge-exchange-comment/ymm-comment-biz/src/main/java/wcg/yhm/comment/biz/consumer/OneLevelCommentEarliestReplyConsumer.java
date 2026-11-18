package wcg.yhm.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.phantomthief.collection.BufferTrigger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import wcg.yhm.comment.biz.constants.MQConstants;
import wcg.yhm.comment.biz.constants.RedisKeyConstants;
import wcg.yhm.comment.biz.domain.dto.CountCommentMqDTO;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.comment.biz.enums.CommentLevelEnum;
import wcg.yhm.comment.biz.service.ICommentService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: 一级评论最早回复字段更新消费者
 * @Version: 1.0
 */

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "ymm_group_earliest_reply_" + MQConstants.TOPIC_COMMENT_COUNT,
topic = MQConstants.TOPIC_COMMENT_COUNT)
public class OneLevelCommentEarliestReplyConsumer implements RocketMQListener<String> {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ICommentService commentService;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

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
        log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, size: {}", bodys.size());
        log.info("==> 【一级评论 first_reply_comment_id 更新】聚合消息, {}", JsonUtil.toJsonString(bodys));

        // 将聚合后的消息体 Json 转 List<CountPublishCommentMqDTO>
        List<CountCommentMqDTO> publishCommentMqDTOS = new ArrayList<>();
        bodys.forEach(body -> {
            try {
                List<CountCommentMqDTO> list = JsonUtil.str2List(body, CountCommentMqDTO.class);
                publishCommentMqDTOS.addAll(list);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        // 过滤出二级评论的 parent_id（即一级评论 ID），并去重，需要更新对应一级评论的 first_reply_comment_id
        List<Long> parentIds = publishCommentMqDTOS.stream()
                .filter(publishCommentMqDTO -> Objects.equals(publishCommentMqDTO.getLevel(), CommentLevelEnum.TWO.getCode()))
                .map(CountCommentMqDTO::getParentId)
                .distinct() // 去重
                .toList();

        if (CollUtil.isEmpty(parentIds)) return;
        // 构建 Redis Key
        List<String> keys = parentIds.stream()
                .map(RedisKeyConstants::buildHaveEarliestReplyCommentKey).toList();

        // 批量查询 Redis
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        // 提取 Redis 中不存在的评论 ID
        List<Long> missingCommentIds = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (Objects.isNull(values.get(i))) {
                missingCommentIds.add(parentIds.get(i));
            }
        }
        // 存在的一级评论 ID，说明表中对应记录的 first_reply_comment_id 已经有值
        if (CollUtil.isNotEmpty(missingCommentIds)) {
            //不存在的，则需要进一步查询数据库来确定，是否要更新记录对应的 first_reply_comment_id 值
            List<Comment> missingComments = commentService.lambdaQuery()
                    .select(Comment::getId, Comment::getLevel, Comment::getParentId, Comment::getUserId, Comment::getFirstReplyCommentId)
                    .in(Comment::getId, missingCommentIds).list();
            //异步redis构建key
            taskExecutor.submit(() -> {
                //过滤出db中 first_reply_comment_id 是 1 的记录
                List<Long> needBuildKeyIds = missingComments.stream()
                        .filter(comment -> comment.getFirstReplyCommentId() != 0)
                        .map(Comment::getId).toList();
                sync2Redis(needBuildKeyIds);
            });
            //过滤出 first_reply_comment_id 是 0 的进行更新
            List<Long> needUpdateIds = missingComments.stream()
                    .filter(comment -> comment.getFirstReplyCommentId() == 0)
                    .map(Comment::getId).toList();
            //查询最早子评论id
            List<Long> redisKeyIds = new ArrayList<>();
            needUpdateIds.forEach(id -> {
                Comment childComment = commentService.lambdaQuery()
                        .eq(Comment::getParentId, id)
                        .orderByAsc(Comment::getCreateTime)
                        .one();
                if (Objects.nonNull(childComment)) {
                    //进行更新
                    boolean result = commentService.lambdaUpdate()
                            .eq(Comment::getId, id)
                            .set(Comment::getFirstReplyCommentId, childComment.getId()).update();
                    if (result) {
                        redisKeyIds.add(id);
                    }
                }
            });
            if (CollUtil.isNotEmpty(redisKeyIds)) {
                sync2Redis(redisKeyIds);
            }
        }
    }

    /**
     * 同步到 Redis 中
     *
     * @param needBuildKeyIds
     */
    private void sync2Redis(List<Long> needBuildKeyIds) {
        // 获取 ValueOperations
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 使用 RedisTemplate 的管道模式，允许在一个操作中批量发送多个命令，防止频繁操作 Redis
        redisTemplate.executePipelined((RedisCallback<?>) (connection) -> {
            needBuildKeyIds.forEach(needSyncCommentId -> {
                // 构建 Redis Key
                String key = RedisKeyConstants.buildHaveEarliestReplyCommentKey(needSyncCommentId);

                // 批量设置值并指定过期时间（5小时以内）
                valueOperations.set(key, 1, RandomUtil.randomInt(5 * 60 * 60), TimeUnit.SECONDS);
            });
            return null;
        });
    }
}
    
