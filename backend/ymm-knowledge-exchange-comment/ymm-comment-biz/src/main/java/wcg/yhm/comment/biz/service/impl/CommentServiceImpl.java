package wcg.yhm.comment.biz.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.comment.biz.constants.MQConstants;
import wcg.yhm.comment.biz.constants.RedisKeyConstants;
import wcg.yhm.comment.biz.domain.dto.CommentPageCacheDTO;
import wcg.yhm.comment.biz.domain.dto.PublishCommentMqDTO;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.comment.biz.domain.po.CommentLike;
import wcg.yhm.comment.biz.domain.request.PublishCommentRequest;
import wcg.yhm.comment.biz.domain.request.QueryCommentListRequest;
import wcg.yhm.comment.biz.domain.request.QueryReplyCommentListRequest;
import wcg.yhm.comment.biz.domain.response.CommentItemResponse;
import wcg.yhm.comment.biz.domain.response.PublishCommentResponse;
import wcg.yhm.comment.biz.enums.CommentLevelEnum;
import wcg.yhm.comment.biz.enums.CommentLikeEnum;
import wcg.yhm.comment.biz.enums.ErrorCodeEnum;
import wcg.yhm.comment.biz.mapper.CommentLikeMapper;
import wcg.yhm.comment.biz.mapper.CommentMapper;
import wcg.yhm.comment.biz.retry.SendMqRetryHelper;
import wcg.yhm.comment.biz.rpc.DistributedIdGeneratorRpcService;
import wcg.yhm.comment.biz.rpc.TextRpcService;
import wcg.yhm.comment.biz.rpc.UserRpcService;
import wcg.yhm.comment.biz.service.CommentCacheService;
import wcg.yhm.comment.biz.service.ICommentService;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.text.api.domain.dto.FindCommentContentResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 评论表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-04-03
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {
    @Resource
    private SendMqRetryHelper sendMqRetryHelper;

    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource
    private TextRpcService textRpcService;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private CommentLikeMapper commentLikeMapper;

    @Resource
    private CommentCacheService commentCacheService;

    @Override
    public PublishCommentResponse publishComment(PublishCommentRequest publishCommentRequest) {
        Long userId = UserContextHolder.getUserId(); //当前登陆用户id
        String content = publishCommentRequest.getContent();
        String imgUrl = publishCommentRequest.getImgUrl();
        Long noteId = publishCommentRequest.getNoteId();
        Long replyCommentId = publishCommentRequest.getReplyCommentId();
        if (StrUtil.isBlank(imgUrl) && StrUtil.isBlank(content)) {
            //如果图片和评论内容都为空抛出异常
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //生成评论id
        String commentId = distributedIdGeneratorRpcService.generateCommentId();
        LocalDateTime createTime = LocalDateTime.now();
        // 先生成评论 ID，再通过 MQ 异步落库；发送成功后即可把 ID 返回给客户端。
        PublishCommentMqDTO commentMqDTO = PublishCommentMqDTO.builder()
                .noteId(noteId)
                .imageUrl(imgUrl)
                .createTime(createTime)
                .replyCommentId(replyCommentId)
                .content(content)
                .creatorId(userId)
                .commentId(Long.valueOf(commentId))
                .build();

        //使用重试框架
        sendMqRetryHelper.syncSend(MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtil.toJsonString(commentMqDTO));
        return PublishCommentResponse.builder()
                .commentId(Long.valueOf(commentId))
                .createTime(createTime)
                .build();

    }

    @Override
    public CursorPageResponse<CommentItemResponse> listComments(QueryCommentListRequest request) {
        // 缓存只存公开的评论分页数据；当前用户的点赞状态在返回前单独补充。
        int limit = normalizeLimit(request.getLimit());
        Long version = commentCacheService.getOneLevelVersion(request.getNoteId());
        String cacheKey = RedisKeyConstants.buildOneLevelPageKey(request.getNoteId(), version, request.getCursor(), limit);
        CommentPageCacheDTO page = commentCacheService.getPage(cacheKey);
        if (Objects.isNull(page)) {
            page = queryOneLevelCommentPage(request.getNoteId(), request.getCursor(), limit);
            commentCacheService.savePage(cacheKey, page);
        }
        return buildUserCursorPage(page);
    }

    @Override
    public CursorPageResponse<CommentItemResponse> listReplyComments(QueryReplyCommentListRequest request) {
        // 回复列表按父评论分别维护版本号，新增回复时可单独让对应页面失效。
        int limit = normalizeLimit(request.getLimit());
        Long version = commentCacheService.getReplyVersion(request.getParentId());
        String cacheKey = RedisKeyConstants.buildReplyPageKey(request.getNoteId(), request.getParentId(), version, request.getCursor(), limit);
        CommentPageCacheDTO page = commentCacheService.getPage(cacheKey);
        if (Objects.isNull(page)) {
            List<Comment> comments = commentMapper.selectReplyByCursor(request.getNoteId(), request.getParentId(), request.getCursor(), limit + 1);
            page = buildPublicCursorPage(request.getNoteId(), comments, limit);
            commentCacheService.savePage(cacheKey, page);
        }
        return buildUserCursorPage(page);
    }

    private int normalizeLimit(Integer limit) {
        if (Objects.isNull(limit)) {
            return 20;
        }
        return Math.min(Math.max(limit, 1), 50);
    }

    private CommentPageCacheDTO queryOneLevelCommentPage(Long noteId, Long cursor, int limit) {
        BigDecimal cursorHeat = null;
        if (Objects.nonNull(cursor)) {
            Comment cursorComment = lambdaQuery()
                    .select(Comment::getId, Comment::getHeat)
                    .eq(Comment::getNoteId, noteId)
                    .eq(Comment::getLevel, CommentLevelEnum.ONE.getCode())
                    .eq(Comment::getParentId, noteId)
                    .eq(Comment::getId, cursor)
                    .one();
            if (Objects.isNull(cursorComment)) {
                return CommentPageCacheDTO.empty();
            }
            cursorHeat = Objects.isNull(cursorComment.getHeat()) ? BigDecimal.ZERO : cursorComment.getHeat();
        }
        List<Comment> comments = commentMapper.selectOneLevelByCursor(noteId, cursor, cursorHeat, limit + 1);
        return buildPublicCursorPage(noteId, comments, limit);
    }

    private CommentPageCacheDTO buildPublicCursorPage(Long noteId, List<Comment> comments, int limit) {
        if (CollUtil.isEmpty(comments)) {
            return CommentPageCacheDTO.empty();
        }

        boolean hasNext = comments.size() > limit;
        List<Comment> pageComments = new ArrayList<>(comments);
        if (hasNext) {
            pageComments.remove(pageComments.size() - 1);
        }
        Long nextCursor = pageComments.isEmpty() ? null : pageComments.get(pageComments.size() - 1).getId();

        List<CommentItemResponse> responses = buildPublicCommentItems(noteId, pageComments);
        return CommentPageCacheDTO.builder()
                .data(responses)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(responses.size())
                .build();
    }

    private CursorPageResponse<CommentItemResponse> buildUserCursorPage(CommentPageCacheDTO page) {
        if (Objects.isNull(page) || CollUtil.isEmpty(page.getData())) {
            return CursorPageResponse.success(Collections.emptyList(), null, false, 0);
        }

        Map<Long, Boolean> commentLikeMap = queryCommentLikeMap(page.getData());
        List<CommentItemResponse> responses = page.getData().stream()
                .map(comment -> copyWithLikeState(comment, Boolean.TRUE.equals(commentLikeMap.get(comment.getId()))))
                .toList();
        return CursorPageResponse.success(responses, page.getNextCursor(), page.isHasNext(), page.getSize());
    }

    private CommentItemResponse copyWithLikeState(CommentItemResponse comment, boolean isLike) {
        return CommentItemResponse.builder()
                .id(comment.getId())
                .noteId(comment.getNoteId())
                .userId(comment.getUserId())
                .nickname(comment.getNickname())
                .avatar(comment.getAvatar())
                .content(comment.getContent())
                .imageUrl(comment.getImageUrl())
                .level(comment.getLevel())
                .parentId(comment.getParentId())
                .replyCommentId(comment.getReplyCommentId())
                .replyUserId(comment.getReplyUserId())
                .replyNickname(comment.getReplyNickname())
                .childCommentTotal(comment.getChildCommentTotal())
                .likeTotal(comment.getLikeTotal())
                .isLike(isLike)
                .createTime(comment.getCreateTime())
                .build();
    }

    private List<CommentItemResponse> buildPublicCommentItems(Long noteId, List<Comment> comments) {
        if (CollUtil.isEmpty(comments)) {
            return Collections.emptyList();
        }

        Map<String, String> contentMap = queryContentMap(noteId, comments);
        Map<Long, FindUserResponse> userMap = queryUserMap(comments);

        return comments.stream()
                .map(comment -> {
                    FindUserResponse user = userMap.get(comment.getUserId());
                    FindUserResponse replyUser = userMap.get(comment.getReplyUserId());
                    String content = Boolean.TRUE.equals(comment.getIsContentEmpty())
                            ? ""
                            : contentMap.getOrDefault(comment.getContentUuid(), "");
                    return CommentItemResponse.builder()
                            .id(comment.getId())
                            .noteId(comment.getNoteId())
                            .userId(comment.getUserId())
                            .nickname(user == null ? null : user.getNickname())
                            .avatar(user == null ? null : user.getAvatar())
                            .content(content)
                            .imageUrl(comment.getImageUrl())
                            .level(comment.getLevel())
                            .parentId(comment.getParentId())
                            .replyCommentId(comment.getReplyCommentId())
                            .replyUserId(comment.getReplyUserId())
                            .replyNickname(replyUser == null ? null : replyUser.getNickname())
                            .childCommentTotal(comment.getChildCommentTotal())
                            .likeTotal(Objects.isNull(comment.getLikeTotal()) ? 0L : comment.getLikeTotal())
                            .isLike(false)
                            .createTime(comment.getCreateTime())
                            .build();
                })
                .toList();
    }

    private Map<String, String> queryContentMap(Long noteId, List<Comment> comments) {
        List<Comment> contentComments = comments.stream()
                .filter(comment -> Boolean.FALSE.equals(comment.getIsContentEmpty()))
                .filter(comment -> StrUtil.isNotBlank(comment.getContentUuid()))
                .toList();
        if (CollUtil.isEmpty(contentComments)) {
            return Collections.emptyMap();
        }

        List<FindCommentContentResponse> contents = textRpcService.batchFindCommentContent(noteId, contentComments);
        if (CollUtil.isEmpty(contents)) {
            return Collections.emptyMap();
        }
        return contents.stream()
                .collect(Collectors.toMap(FindCommentContentResponse::getContentId,
                        FindCommentContentResponse::getContent,
                        (first, second) -> first));
    }

    private Map<Long, FindUserResponse> queryUserMap(List<Comment> comments) {
        List<Long> userIds = new ArrayList<>();
        comments.forEach(comment -> {
            if (Objects.nonNull(comment.getUserId()) && comment.getUserId() > 0) {
                userIds.add(comment.getUserId());
            }
            if (Objects.nonNull(comment.getReplyUserId()) && comment.getReplyUserId() > 0) {
                userIds.add(comment.getReplyUserId());
            }
        });

        List<FindUserResponse> users = userRpcService.batchQueryUser(userIds);
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream()
                .collect(Collectors.toMap(FindUserResponse::getId, Function.identity(), (first, second) -> first));

    }

    private Map<Long, Boolean> queryCommentLikeMap(List<CommentItemResponse> comments) {
        Long userId = UserContextHolder.getUserId();
        if (Objects.isNull(userId) || CollUtil.isEmpty(comments)) {
            return Collections.emptyMap();
        }
        List<Long> commentIds = comments.stream()
                .map(CommentItemResponse::getId)
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(commentIds)) {
            return Collections.emptyMap();
        }
        List<CommentLike> commentLikes = commentLikeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommentLike>()
                        .select(CommentLike::getCommentId)
                        .eq(CommentLike::getUserId, userId)
                        .eq(CommentLike::getStatus, CommentLikeEnum.LIKE.getStatus())
                        .in(CommentLike::getCommentId, commentIds));
        if (CollUtil.isEmpty(commentLikes)) {
            return Collections.emptyMap();
        }
        return commentLikes.stream()
                .collect(Collectors.toMap(CommentLike::getCommentId, like -> true, (first, second) -> first));
    }
}
