package wcg.yhm.note.service.impl;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import wcg.yhm.note.domain.mapper.NoteMapper;
import wcg.yhm.note.enums.NoteStatusEnum;
import wcg.yhm.note.enums.NoteVisibleEnum;
import wcg.yhm.note.model.vo.AdminChannelWordCloudResponse;
import wcg.yhm.note.model.vo.AdminDashboardPeriodResponse;
import wcg.yhm.note.model.vo.AdminDashboardResponse;
import wcg.yhm.note.model.vo.AdminLikedNoteResponse;
import wcg.yhm.note.model.vo.AdminUserRankResponse;
import wcg.yhm.note.rpc.UserRelationRpcService;
import wcg.yhm.note.rpc.UserRpcService;
import wcg.yhm.note.service.IAdminDashboardService;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.user.relation.api.domain.response.FanRankResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端分析面板聚合实现。
 */
@Service
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private static final int RANK_LIMIT = 10;

    @Resource
    private NoteMapper noteMapper;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private UserRelationRpcService userRelationRpcService;

    @Override
    public AdminDashboardResponse analysis() {
        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setLast7Days(buildPeriod(7));
        response.setLast30Days(buildPeriod(30));

        List<AdminLikedNoteResponse> topLikedNotes = safeList(noteMapper.selectTopLikedNotes(
                NoteStatusEnum.NORMAL.getValue(), NoteVisibleEnum.PUBLIC.getValue(), RANK_LIMIT));
        enrichLikedNotes(topLikedNotes);
        response.setTopLikedNotes(topLikedNotes);
        return response;
    }

    private AdminDashboardPeriodResponse buildPeriod(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1L).atStartOfDay();

        List<AdminUserRankResponse> topFans = userRelationRpcService
                .adminFanRank(startTime, endTime, RANK_LIMIT)
                .stream()
                .map(this::toUserRank)
                .collect(Collectors.toList());
        List<AdminUserRankResponse> topPublishers = safeList(noteMapper.selectTopPublishers(
                startTime, endTime, NoteStatusEnum.DELETED.getValue(), RANK_LIMIT));
        List<AdminChannelWordCloudResponse> channelWordCloud = safeList(
                noteMapper.selectAdminChannelWordCloud(
                        startTime, endTime, NoteStatusEnum.DELETED.getValue()));

        enrichUserRanks(topFans);
        enrichUserRanks(topPublishers);

        AdminDashboardPeriodResponse period = new AdminDashboardPeriodResponse();
        period.setDays(days);
        period.setStartDate(startDate.toString());
        period.setEndDate(endDate.toString());
        period.setTopFans(topFans);
        period.setTopPublishers(topPublishers);
        period.setChannelWordCloud(channelWordCloud);
        return period;
    }

    private AdminUserRankResponse toUserRank(FanRankResponse source) {
        AdminUserRankResponse target = new AdminUserRankResponse();
        target.setUserId(source.getUserId());
        target.setCount(source.getCount());
        return target;
    }

    private void enrichUserRanks(List<AdminUserRankResponse> ranking) {
        List<Long> userIds = ranking.stream()
                .map(AdminUserRankResponse::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, FindUserResponse> userMap = queryUserMap(userIds);
        ranking.forEach(item -> {
            FindUserResponse user = userMap.get(item.getUserId());
            item.setNickname(user == null ? fallbackUserName(item.getUserId()) : user.getNickname());
            item.setAvatar(user == null ? null : user.getAvatar());
        });
    }

    private void enrichLikedNotes(List<AdminLikedNoteResponse> notes) {
        List<Long> creatorIds = notes.stream()
                .map(AdminLikedNoteResponse::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, FindUserResponse> userMap = queryUserMap(creatorIds);
        notes.forEach(note -> {
            FindUserResponse user = userMap.get(note.getCreatorId());
            note.setCreatorName(user == null ? fallbackUserName(note.getCreatorId()) : user.getNickname());
            note.setCreatorAvatar(user == null ? null : user.getAvatar());
        });
    }

    private Map<Long, FindUserResponse> queryUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRpcService.batchQueryUserRequired(userIds).stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(FindUserResponse::getId, Function.identity(), (left, right) -> left));
    }

    private String fallbackUserName(Long userId) {
        return userId == null ? "未知用户" : "用户 " + userId;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
