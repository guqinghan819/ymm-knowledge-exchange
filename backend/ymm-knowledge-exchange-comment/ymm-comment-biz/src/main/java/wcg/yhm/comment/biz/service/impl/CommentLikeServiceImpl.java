package wcg.yhm.comment.biz.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import wcg.yhm.comment.biz.constants.MQConstants;
import wcg.yhm.comment.biz.domain.dto.CommentLikeUnlikeMqDTO;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.comment.biz.domain.po.CommentLike;
import wcg.yhm.comment.biz.enums.CommentLikeEnum;
import wcg.yhm.comment.biz.enums.ErrorCodeEnum;
import wcg.yhm.comment.biz.mapper.CommentLikeMapper;
import wcg.yhm.comment.biz.service.ICommentLikeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import wcg.yhm.comment.biz.service.ICommentService;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 评论点赞表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-04-03
 */
@Service
@Slf4j
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements ICommentLikeService {

    private static final String COMMENT_LIKE_STATE_KEY_PREFIX = "comment:like:state:";

    private static final long LIKE_STATE_TTL_SECONDS = 60 * 60 * 24 * 2;

    @Resource
    private ICommentService commentService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void likeComment(Long commentId) {
        operateCommentLike(commentId, CommentLikeEnum.LIKE);
    }

    @Override
    public void unlikeComment(Long commentId) {
        operateCommentLike(commentId, CommentLikeEnum.UNLIKE);
    }

    private void operateCommentLike(Long commentId, CommentLikeEnum targetStatus) {
        Long userId = UserContextHolder.getUserId();
        if (Objects.isNull(userId)) {
            throw new BizException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        checkCommentExists(commentId);

        boolean liked = getCurrentLikeState(userId, commentId);
        if (targetStatus == CommentLikeEnum.LIKE && liked) {
            throw new BizException(ErrorCodeEnum.COMMENT_ALREADY_LIKE);
        }
        if (targetStatus == CommentLikeEnum.UNLIKE && !liked) {
            throw new BizException(ErrorCodeEnum.COMMENT_NOT_LIKE);
        }

        cacheLikeState(userId, commentId, targetStatus == CommentLikeEnum.LIKE);
        sendLikeMessage(userId, commentId, targetStatus);
    }

    private void checkCommentExists(Long commentId) {
        boolean exists = commentService.lambdaQuery()
                .eq(Comment::getId, commentId)
                .exists();
        if (!exists) {
            throw new BizException(ErrorCodeEnum.COMMENT_NOT_FOUND);
        }
    }

    private boolean getCurrentLikeState(Long userId, Long commentId) {
        String stateKey = buildLikeStateKey(userId, commentId);
        Object cachedStatus = redisTemplate.opsForValue().get(stateKey);
        if (Objects.nonNull(cachedStatus)) {
            return Objects.equals(String.valueOf(CommentLikeEnum.LIKE.getStatus()), String.valueOf(cachedStatus));
        }

        return lambdaQuery()
                .eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getStatus, CommentLikeEnum.LIKE.getStatus())
                .exists();
    }

    private void cacheLikeState(Long userId, Long commentId, boolean liked) {
        String stateKey = buildLikeStateKey(userId, commentId);
        redisTemplate.opsForValue().set(stateKey,
                liked ? CommentLikeEnum.LIKE.getStatus() : CommentLikeEnum.UNLIKE.getStatus(),
                LIKE_STATE_TTL_SECONDS,
                TimeUnit.SECONDS);
    }

    private String buildLikeStateKey(Long userId, Long commentId) {
        return COMMENT_LIKE_STATE_KEY_PREFIX + userId + ":" + commentId;
    }

    private void sendLikeMessage(Long userId, Long commentId, CommentLikeEnum status) {
        CommentLikeUnlikeMqDTO dto = new CommentLikeUnlikeMqDTO(status.getStatus(), commentId, userId, LocalDateTime.now());
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(dto)).build();
        String tag = status == CommentLikeEnum.LIKE ? MQConstants.TAG_LIKE : MQConstants.TAG_UNLIKE;
        String destination = MQConstants.TOPIC_COMMENT_LIKE_OR_UNLIKE + ":" + tag;
        String hashKey = String.valueOf(userId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 评论点赞操作 MQ 发送成功, commentId: {}, userId: {}, status: {}, result: {}",
                        commentId, userId, status.getStatus(), sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 评论点赞操作 MQ 发送失败, commentId: {}, userId: {}, status: {}",
                        commentId, userId, status.getStatus(), throwable);
            }
        });
    }
}
