package wcg.yhm.comment.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.comment.biz.constants.MQConstants;
import wcg.yhm.comment.biz.domain.bo.CommentHeatBO;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.comment.biz.mapper.CommentMapper;
import wcg.yhm.comment.biz.service.CommentCacheService;
import wcg.yhm.comment.biz.service.ICommentService;
import wcg.yhm.comment.biz.utils.HeatCalculator;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-05
 * @Description: 评论热度更新消费者
 * @Version: 1.0
 */

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_COMMENT_HEAT_UPDATE,
topic = MQConstants.TOPIC_COMMENT_HEAT_UPDATE)
public class CommentHeatUpdateConsumer implements RocketMQListener<String> {
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private ICommentService commentService;
    @Resource
    private CommentCacheService commentCacheService;
    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(300)   // 一批次最多聚合 300 条
            .linger(Duration.ofSeconds(2)) // 多久聚合一次（2s 一次）
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【评论热度值计算】聚合消息, size: {}", bodys.size());
        log.info("==> 【评论热度值计算】聚合消息, {}", JsonUtil.toJsonString(bodys));

        // 将聚合后的消息体 Json 转 Set<Long>, 去重相同的评论 ID, 防止重复计算
        Set<Long> commentIds = new HashSet<>();
        bodys.forEach(body -> {
            try {
                Set<Long> list = JsonUtil.parseSet(body, Long.class);
                commentIds.addAll(list);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        log.info("==> 去重后的评论 ID: {}", commentIds);
        if (commentIds.isEmpty()) {
            return;
        }
        // 批量查询评论
        List<Comment> comments = commentService.lambdaQuery()
                .select(Comment::getId, Comment::getLevel, Comment::getParentId, Comment::getUserId,
                        Comment::getNoteId, Comment::getLikeTotal, Comment::getChildCommentTotal)
                .in(Comment::getId, commentIds)
                .list();
        // 评论 ID
        List<Long> ids = new ArrayList<>();
        // 热度值 BO
        List<CommentHeatBO> commentBOS = new ArrayList<>();
        // 重新计算每条评论的热度值
        comments.forEach(comment -> {
            Long commentId = comment.getId();
            // 被点赞数
            Long likeTotal = Objects.isNull(comment.getLikeTotal()) ? 0L : comment.getLikeTotal();
            // 被回复数
            Long childCommentTotal = Objects.isNull(comment.getChildCommentTotal()) ? 0L : comment.getChildCommentTotal();

            // 计算热度值
            BigDecimal heatNum = HeatCalculator.calculateHeat(likeTotal, childCommentTotal);
            ids.add(commentId);
            commentBOS.add(CommentHeatBO.builder()
                    .id(commentId)
                    .heat(heatNum.doubleValue())
                    .build());
        });

        // 批量更新评论热度值
        if (!ids.isEmpty()) {
            commentMapper.batchUpdateHeatByCommentIds(ids, commentBOS);
            commentCacheService.increaseVersionsByComments(comments);
        }
    }
}
    
