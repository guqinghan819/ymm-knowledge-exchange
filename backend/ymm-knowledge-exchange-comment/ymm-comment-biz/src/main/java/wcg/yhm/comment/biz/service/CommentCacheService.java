package wcg.yhm.comment.biz.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import wcg.yhm.comment.biz.constants.RedisKeyConstants;
import wcg.yhm.comment.biz.domain.dto.CommentPageCacheDTO;
import wcg.yhm.comment.biz.domain.po.Comment;
import wcg.yhm.comment.biz.enums.CommentLevelEnum;
import wcg.yhm.comment.biz.utils.CommentCacheUtil;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 评论分页多级缓存。
 */
@Service
@Slf4j
public class CommentCacheService {

    private static final int REDIS_TTL_BASE_SECONDS = 60;

    private static final int REDIS_TTL_RANDOM_SECONDS = 60;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public Long getOneLevelVersion(Long noteId) {
        return getVersion(RedisKeyConstants.buildOneLevelVersionKey(noteId));
    }

    public Long getReplyVersion(Long parentId) {
        return getVersion(RedisKeyConstants.buildReplyVersionKey(parentId));
    }

    public CommentPageCacheDTO getPage(String cacheKey) {
        CommentPageCacheDTO localPage = CommentCacheUtil.getCommentPage(cacheKey);
        if (Objects.nonNull(localPage)) {
            return localPage;
        }

        try {
            Object redisValue = redisTemplate.opsForValue().get(cacheKey);
            if (Objects.isNull(redisValue)) {
                return null;
            }
            CommentPageCacheDTO page;
            if (redisValue instanceof CommentPageCacheDTO cacheDTO) {
                page = cacheDTO;
            } else if (redisValue instanceof String value) {
                page = JsonUtil.parseObject(value, CommentPageCacheDTO.class);
            } else {
                page = JsonUtil.parseObject(JsonUtil.toJsonString(redisValue), CommentPageCacheDTO.class);
            }
            if (Objects.nonNull(page)) {
                CommentCacheUtil.saveCommentPage(cacheKey, page);
            }
            return page;
        } catch (Exception e) {
            log.warn("==> 读取评论分页缓存失败, key: {}", cacheKey, e);
            return null;
        }
    }

    public void savePage(String cacheKey, CommentPageCacheDTO page) {
        if (Objects.isNull(page)) {
            return;
        }
        CommentCacheUtil.saveCommentPage(cacheKey, page);
        try {
            int ttl = REDIS_TTL_BASE_SECONDS + RandomUtil.randomInt(REDIS_TTL_RANDOM_SECONDS);
            redisTemplate.opsForValue().set(cacheKey, JsonUtil.toJsonString(page), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("==> 写入评论分页缓存失败, key: {}", cacheKey, e);
        }
    }

    public void increaseOneLevelVersion(Long noteId) {
        increaseVersion(RedisKeyConstants.buildOneLevelVersionKey(noteId));
    }

    public void increaseReplyVersion(Long parentId) {
        increaseVersion(RedisKeyConstants.buildReplyVersionKey(parentId));
    }

    public void increasePublishVersions(Long noteId, Integer level, Long parentId) {
        increaseOneLevelVersion(noteId);
        if (Objects.equals(level, CommentLevelEnum.TWO.getCode())) {
            increaseReplyVersion(parentId);
        }
    }

    public void increaseVersionsByComments(List<Comment> comments) {
        if (CollUtil.isEmpty(comments)) {
            return;
        }
        comments.forEach(comment -> {
            if (Objects.equals(comment.getLevel(), CommentLevelEnum.ONE.getCode())) {
                increaseOneLevelVersion(comment.getNoteId());
                return;
            }
            if (Objects.equals(comment.getLevel(), CommentLevelEnum.TWO.getCode())) {
                increaseReplyVersion(comment.getParentId());
            }
        });
    }

    private Long getVersion(String versionKey) {
        Object version = redisTemplate.opsForValue().get(versionKey);
        if (Objects.isNull(version)) {
            return 0L;
        }
        return Long.valueOf(String.valueOf(version));
    }

    private void increaseVersion(String versionKey) {
        try {
            redisTemplate.opsForValue().increment(versionKey);
        } catch (Exception e) {
            log.warn("==> 更新评论缓存版本失败, key: {}", versionKey, e);
        }
    }
}
