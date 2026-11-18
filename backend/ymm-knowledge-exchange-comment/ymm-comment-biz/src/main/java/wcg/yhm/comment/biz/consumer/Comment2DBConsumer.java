package wcg.yhm.comment.biz.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import wcg.yhm.comment.biz.constants.MQConstants;
import wcg.yhm.comment.biz.domain.bo.CommentBO;
import wcg.yhm.comment.biz.domain.dto.CountCommentMqDTO;
import wcg.yhm.comment.biz.domain.dto.PublishCommentMqDTO;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.comment.biz.enums.CommentLevelEnum;
import wcg.yhm.comment.biz.mapper.CommentMapper;
import wcg.yhm.comment.biz.service.CommentCacheService;
import wcg.yhm.comment.biz.rpc.TextRpcService;
import wcg.yhm.comment.biz.service.ICommentService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description: 评论落库消费者
 * @Version: 1.0
 */

@Component
@Slf4j
public class Comment2DBConsumer {

    @Value("${rocketmq.name-server}")
    private String nameServerAddr;

    private DefaultMQPushConsumer consumer;

    @Resource
    private ICommentService commentService;

    //每秒创建1000个令牌
    private RateLimiter rateLimiter = RateLimiter.create(1000);

    @Resource
    private TextRpcService textRpcService;

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private CommentCacheService commentCacheService;
    @Bean
    public DefaultMQPushConsumer mqPushConsumer() throws MQClientException {
        String group = "ymm_group_" + MQConstants.TOPIC_PUBLISH_COMMENT;
        //指定组
        consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(nameServerAddr);
        //订阅主题,并设置订阅主题规则, * 表示订阅该主题下的所有标签
        consumer.subscribe(MQConstants.TOPIC_PUBLISH_COMMENT, "*");
        //设置消费者消费消息的起始位置,如果队列中没有,就从最新的消息开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        //设置消息消费模式,设置为集群
        consumer.setMessageModel(MessageModel.CLUSTERING);
        //设置消费者每批次最大消费数量
        consumer.setConsumeMessageBatchMaxSize(30);
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            log.info("==========> 本批次消息大小: {}", msgs.size());
            try {
                //获取令牌
                rateLimiter.acquire();
                List<PublishCommentMqDTO> list = new ArrayList<>();
                for (MessageExt msg : msgs) {
                    String message = new String(msg.getBody());
                    log.info("==========> Consumer - Received message: {}", message);
                    //jsonStr -> dto
                    list.add(JsonUtil.parseObject(message, PublishCommentMqDTO.class));
                }
                list = new ArrayList<>(list.stream()
                        .filter(dto -> Objects.nonNull(dto.getCommentId()))
                        .collect(Collectors.toMap(PublishCommentMqDTO::getCommentId,
                                dto -> dto,
                                (first, second) -> first,
                                LinkedHashMap::new))
                        .values());

                List<Long> commentIds = list.stream()
                        .map(PublishCommentMqDTO::getCommentId)
                        .filter(Objects::nonNull)
                        .toList();
                Set<Long> existsCommentIds = new HashSet<>();
                if (CollUtil.isNotEmpty(commentIds)) {
                    existsCommentIds = commentService.lambdaQuery()
                            .select(Comment::getId)
                            .in(Comment::getId, commentIds)
                            .list()
                            .stream()
                            .map(Comment::getId)
                            .collect(Collectors.toSet());
                }
                if (CollUtil.isNotEmpty(existsCommentIds)) {
                    Set<Long> finalExistsCommentIds = existsCommentIds;
                    list = list.stream()
                            .filter(dto -> !finalExistsCommentIds.contains(dto.getCommentId()))
                            .toList();
                    log.info("==> 过滤重复评论消息后剩余数量: {}", list.size());
                }
                if (CollUtil.isEmpty(list)) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }

                //提取所有不为空的回复评论id
                List<Long> replyCommentIds = list.stream()
                        .filter(publishCommentMqDTO -> Objects.nonNull(publishCommentMqDTO.getReplyCommentId()))
                        .map(PublishCommentMqDTO::getReplyCommentId).toList();

                List<Comment> replyComments = new ArrayList<>();

                if (CollUtil.isNotEmpty(replyCommentIds)) {
                    //批量查询回复相关评论
                     replyComments = commentService.lambdaQuery()
                            .select(Comment::getId, Comment::getNoteId, Comment::getLevel, Comment::getParentId, Comment::getUserId)
                            .in(Comment::getId, replyCommentIds).list();
                }
                Map<Long, Comment> idWithCommentMap = new HashMap<>();
                if (CollUtil.isNotEmpty(replyComments)) {
                    idWithCommentMap = replyComments.stream()
                            .collect(Collectors.toMap(Comment::getId, c -> c));
                }
                //dto -> bo
                List<CommentBO> commentBOList = new ArrayList<>();
                for (PublishCommentMqDTO dto : list) {
                    String imgUrl = dto.getImageUrl();
                    CommentBO commentBO = CommentBO.builder()
                            .id(dto.getCommentId())
                            .imageUrl(imgUrl)
                            .noteId(dto.getNoteId())
                            .userId(dto.getCreatorId())
                            .isContentEmpty(true) //默认评论内容为空
                            .level(CommentLevelEnum.ONE.getCode()) //默认为一级评论
                            .parentId(dto.getNoteId()) //默认设置所属笔记id
                            .createTime(dto.getCreateTime())
                            .updateTime(dto.getCreateTime())
                            .isTop(0)
                            .replyTotal(0L)
                            .likeTotal(0L)
                            .replyCommentId(0L)
                            .replyUserId(0L)
                            .build();

                    //评论内容不为空
                    String content = dto.getContent();
                    if (StrUtil.isNotBlank(content)) {
                        commentBO.setContent(content);
                        commentBO.setIsContentEmpty(false);
                        commentBO.setContentUuid(UUID.randomUUID().toString()); //生成评论内容的id
                    }

                    //存在回复情况
                    Long replyCommentId = dto.getReplyCommentId();
                    if (Objects.nonNull(replyCommentId)) {
                        //获取回复内容
                        Comment replyComment = idWithCommentMap.get(replyCommentId);
                        if (Objects.isNull(replyComment)) {
                            log.warn("==> 回复评论不存在, 跳过本条评论, commentId: {}, replyCommentId: {}",
                                    dto.getCommentId(), replyCommentId);
                            continue;
                        }
                        if (!Objects.equals(replyComment.getNoteId(), dto.getNoteId())) {
                            log.warn("==> 回复评论不属于当前笔记, 跳过本条评论, commentId: {}, noteId: {}, replyCommentId: {}, replyNoteId: {}",
                                    dto.getCommentId(), dto.getNoteId(), replyCommentId, replyComment.getNoteId());
                            continue;
                        }
                        //说明是二级评论
                        commentBO.setLevel(CommentLevelEnum.TWO.getCode());
                        commentBO.setReplyCommentId(dto.getReplyCommentId());
                        commentBO.setParentId(replyComment.getId());
                        if (Objects.equals(replyComment.getLevel(), CommentLevelEnum.TWO.getCode())) {
                            commentBO.setParentId(replyComment.getParentId());
                        }
                        commentBO.setReplyUserId(replyComment.getUserId());
                    }
                    commentBOList.add(commentBO);
                }
                if (CollUtil.isEmpty(commentBOList)) {
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                log.info("## 清洗后的 CommentBOList: {}", JsonUtil.toJsonString(commentBOList));
                Integer rows = transactionTemplate.execute(status -> {
                    try {
                        int affectRows = commentMapper.batchInsert(commentBOList);
                        //过滤出内容不为空的bo
                        List<CommentBO> haveContentBOS = commentBOList.stream()
                                .filter(bo -> Boolean.FALSE.equals(bo.getIsContentEmpty())).toList();
                        if (CollUtil.isNotEmpty(haveContentBOS)) {
                            textRpcService.batchAddComment(haveContentBOS);
                        }
                        return affectRows;
                    } catch (Exception e) {
                        status.setRollbackOnly(); //标记事务回滚
                        log.error("ex: ", e);
                        throw e;
                    }
                });
                if (Objects.nonNull(rows) && rows > 0) {
                    commentBOList.forEach(commentBO -> commentCacheService.increasePublishVersions(
                            commentBO.getNoteId(),
                            commentBO.getLevel(),
                            commentBO.getParentId()));
                    //发送评论计数
                    // 构建发送给计数服务的 DTO 集合
                    List<CountCommentMqDTO> countPublishCommentMqDTOS = commentBOList.stream()
                            .map(commentBO -> CountCommentMqDTO.builder()
                                    .noteId(commentBO.getNoteId())
                                    .commentId(commentBO.getId())
                                    .level(commentBO.getLevel())
                                    .parentId(commentBO.getParentId())
                                    .build())
                            .toList();
                    // 异步发送计数 MQ
                    org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(countPublishCommentMqDTOS))
                            .build();

                    // 异步发送 MQ 消息
                    rocketMQTemplate.asyncSend(MQConstants.TOPIC_COMMENT_COUNT, message, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("==> 【计数: 评论发布】MQ 发送成功，SendResult: {}", sendResult);
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            log.error("==> 【计数: 评论发布】MQ 发送异常: ", throwable);
                        }
                    });
                }
                //手动ack, 告诉RocketMQ这批次消息消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                log.error("error:", e);
                //告诉rocketmq这批次消息消费失败,需要进行重试
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        });
        //启动消费者
        consumer.start();
        return consumer;
    }


    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(consumer)) {
            try {
                consumer.shutdown();  // 关闭消费者
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }
}
    
