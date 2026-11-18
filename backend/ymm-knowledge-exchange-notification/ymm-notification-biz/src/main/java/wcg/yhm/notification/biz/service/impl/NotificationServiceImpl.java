package wcg.yhm.notification.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.notification.api.domain.request.NotificationListRequest;
import wcg.yhm.notification.api.domain.response.NotificationItemResponse;
import wcg.yhm.notification.biz.constants.NotificationConstants;
import wcg.yhm.notification.biz.domain.po.Notification;
import wcg.yhm.notification.biz.enums.ErrorCodeEnum;
import wcg.yhm.notification.biz.mapper.NotificationMapper;
import wcg.yhm.notification.biz.rpc.UserRpcService;
import wcg.yhm.notification.biz.service.INotificationService;
import wcg.yhm.notification.biz.service.NotificationFeedCacheService;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 查询站内通知、维护已读状态；Redis 保存首页 ID 和未读数以加速访问。 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements INotificationService {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int FEED_WARM_SIZE = 500;

    @Resource
    private NotificationFeedCacheService feedCacheService;
    @Resource
    private UserRpcService userRpcService;

    @Override
    public CursorPageResponse<NotificationItemResponse> list(NotificationListRequest request) {
        Long userId = requireUserId();
        int limit = normalizeLimit(request == null ? null : request.getLimit());
        Long cursor = request == null ? null : request.getCursor();

        List<Notification> records = cursor == null
                ? firstPage(userId, limit + 1)
                : queryDatabase(userId, cursor, limit + 1);
        boolean hasNext = records.size() > limit;
        if (hasNext) {
            records = new ArrayList<>(records.subList(0, limit));
        }

        Map<Long, FindUserResponse> users = actorMap(records);
        List<NotificationItemResponse> response = records.stream().map(item -> {
            FindUserResponse actor = users.get(item.getActorId());
            return NotificationItemResponse.builder()
                    .id(item.getId())
                    .type(item.getType())
                    .actorId(item.getActorId())
                    .actorNickname(item.getActorId() == null
                            ? null
                            : (actor == null ? "用户" + item.getActorId() : actor.getNickname()))
                    .actorAvatar(actor == null ? null : actor.getAvatar())
                    .noteId(item.getTargetId())
                    .noteTitle(item.getTargetTitle())
                    .reasonCode(item.getReasonCode())
                    .reason(item.getReason())
                    .isRead(Boolean.TRUE.equals(item.getIsRead()))
                    .createTime(item.getCreateTime())
                    .build();
        }).toList();
        Long nextCursor = response.isEmpty() ? null : response.get(response.size() - 1).getId();
        return CursorPageResponse.success(response, nextCursor, hasNext, response.size());
    }

    @Override
    public Long unreadCount() {
        Long userId = requireUserId();
        Long cached = feedCacheService.getUnread(userId);
        if (cached != null) {
            return cached;
        }
        Long count = lambdaQuery()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE)
                .eq(Notification::getIsRead, false)
                .count();
        feedCacheService.cacheUnread(userId, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void read(Long notificationId) {
        // 更新条件包含接收人、有效状态和未读状态，避免越权或重复更新已读时间。
        Long userId = requireUserId();
        int rows = baseMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadTime, LocalDateTime.now())
                .set(Notification::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            boolean exists = lambdaQuery()
                    .eq(Notification::getId, notificationId)
                    .eq(Notification::getRecipientId, userId)
                    .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE)
                    .exists();
            if (!exists) {
                throw new BizException(ErrorCodeEnum.NOTIFICATION_NOT_FOUND);
            }
        }
        feedCacheService.invalidateUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll() {
        Long userId = requireUserId();
        LocalDateTime now = LocalDateTime.now();
        baseMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadTime, now)
                .set(Notification::getUpdateTime, now));
        feedCacheService.invalidateUnread(userId);
    }

    private List<Notification> firstPage(Long userId, int fetchSize) {
        // Redis 仅保存候选 ID，仍从数据库读取并校验有效性；缓存不完整时重建。
        List<Long> ids = feedCacheService.firstPageIds(userId, fetchSize - 1);
        if (ids != null) {
            List<Notification> cachedRecords = queryByIds(userId, ids);
            if (cachedRecords.size() == ids.size()) {
                return cachedRecords;
            }
        }

        List<Notification> warmRecords = queryDatabase(userId, null, FEED_WARM_SIZE);
        try {
            feedCacheService.rebuild(userId, warmRecords.stream().map(Notification::getId).toList());
        } catch (Exception ignored) {
            // Redis 仅作加速层，失败时继续使用 MySQL 结果。
        }
        return warmRecords.size() > fetchSize
                ? new ArrayList<>(warmRecords.subList(0, fetchSize))
                : warmRecords;
    }

    private List<Notification> queryDatabase(Long userId, Long cursor, int limit) {
        var query = Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getRecipientId, userId)
                .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE);
        if (cursor != null) {
            query.lt(Notification::getId, cursor);
        }
        return baseMapper.selectList(query.orderByDesc(Notification::getId).last("LIMIT " + limit));
    }

    private List<Notification> queryByIds(Long userId, List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Notification> byId = baseMapper.selectList(Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getRecipientId, userId)
                        .eq(Notification::getStatus, NotificationConstants.STATUS_ACTIVE)
                        .in(Notification::getId, ids))
                .stream().collect(Collectors.toMap(Notification::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    private Map<Long, FindUserResponse> actorMap(List<Notification> records) {
        List<Long> actorIds = records.stream()
                .map(Notification::getActorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (actorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRpcService.findByIds(actorIds).stream()
                .collect(Collectors.toMap(FindUserResponse::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
    }

    private int normalizeLimit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_LOGIN);
        }
        return userId;
    }
}
