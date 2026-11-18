package wcg.yhm.user.relation.biz.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import wcg.yhm.user.api.controller.UserFeignApi;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.user.relation.biz.constant.RedisKeyConstants;
import wcg.yhm.user.relation.biz.domain.dto.FindFansRequest;
import wcg.yhm.user.relation.biz.domain.po.Fans;
import wcg.yhm.user.relation.biz.domain.vo.FanAnalysisPeriodResponse;
import wcg.yhm.user.relation.biz.domain.vo.FanAnalysisResponse;
import wcg.yhm.user.relation.biz.domain.vo.FanDailyAddedResponse;
import wcg.yhm.user.relation.biz.domain.vo.FindFansResponse;
import wcg.yhm.user.relation.biz.enums.ErrorCodeEnum;
import wcg.yhm.user.relation.biz.mapper.FansMapper;
import wcg.yhm.user.relation.biz.rpc.UserRpcService;
import wcg.yhm.user.relation.biz.service.IFansService;
import wcg.yhm.user.relation.api.domain.request.AdminFanRankRequest;
import wcg.yhm.user.relation.api.domain.response.FanRankResponse;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户粉丝表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-01-25
 */
@Service
@Slf4j
public class FansServiceImpl extends ServiceImpl<FansMapper, Fans> implements IFansService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private UserRpcService userRpcService;
    @Override
    public PageResponse<FindFansResponse> findFansList(FindFansRequest findFansRequest) {
        Long userId = findFansRequest.getUserId();
        Integer pageNo = findFansRequest.getPageNo();
        long pageSize = 10; //默认显示10个
        List<FindFansResponse> fansList = null;
        String redisKey = RedisKeyConstants.USER_FANS_KEY_PREFIX + userId;
        //查询redis
        Long total = redisTemplate.opsForZSet().zCard(redisKey);
        long totalPage;
        //如果存在直接返回
        if (total != null && total > 0) {
            totalPage = PageResponse.getTotalPage(total, pageSize);
            if (pageNo > totalPage) {
                throw new BizException(ErrorCodeEnum.ERROR_PAGE_NO);
            }
            long offset = PageResponse.getOffset(pageNo, pageSize);
            Set<Object> fansSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(redisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize);
            if (CollUtil.isNotEmpty(fansSet)) {
                List<Long> fansIds = fansSet.stream().map(f -> Long.valueOf(f.toString())).toList();
                //调用rpc查询用户
                List<FindUserResponse> userList = userRpcService.findByIds(fansIds);
                //进行转换
                fansList = userList.stream().map(u -> {
                    FindFansResponse findFansResponse = BeanUtil.copyProperties(u, FindFansResponse.class);
                    findFansResponse.setFansTotal(0L); //todo 后续通过计数服务
                    findFansResponse.setNoteTotal(0L); //todo 后续通过计数服务
                    return findFansResponse;
                }).collect(Collectors.toList());
            }
        } else {
            //不存在就走数据库并同步
            Page<Fans> fansPage = new Page<>(pageNo, pageSize);
            Page<Fans> page = lambdaQuery().eq(Fans::getUserId, userId)
                    .orderByDesc(Fans::getCreateTime)
                    .page(fansPage);
            List<Fans> records = page.getRecords();
            total = page.getTotal();
            List<Long> fansIds = records.stream().map(Fans::getUserId).toList();
            List<FindUserResponse> fansResponse = userRpcService.findByIds(fansIds);
            if (CollUtil.isNotEmpty(fansResponse)) {
                fansList = fansResponse.stream()
                        .map(f -> {
                            FindFansResponse findFansResponse = BeanUtil.copyProperties(f, FindFansResponse.class);
                            findFansResponse.setFansTotal(0L); //todo 后续通过计数服务
                            findFansResponse.setNoteTotal(0L); //todo 后续通过计数服务
                            return findFansResponse;
                        })
                        .toList();

                //异步同步
                threadPoolTaskExecutor.submit(() -> syncFansList2Redis(userId));
            }

        }

        return PageResponse.success(fansList, pageNo, total);
    }

    @Override
    public FanAnalysisResponse analysis() {
        FanAnalysisResponse response = new FanAnalysisResponse();
        response.setLast7Days(buildPeriod(7));
        response.setLast30Days(buildPeriod(30));
        return response;
    }

    @Override
    public List<FanRankResponse> adminFanRank(AdminFanRankRequest request) {
        if (request == null || request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        int limit = request.getLimit() == null ? 10 : Math.min(Math.max(request.getLimit(), 1), 100);
        List<FanRankResponse> ranking = getBaseMapper().selectAdminFanRank(
                request.getStartTime(), request.getEndTime(), limit);
        return ranking == null ? List.of() : ranking;
    }

    private FanAnalysisPeriodResponse buildPeriod(int days) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        List<FanDailyAddedResponse> rawDailyAdded = getBaseMapper().selectDailyAdded(userId, startTime, endTime);
        Map<String, FanDailyAddedResponse> dailyMap = rawDailyAdded.stream()
                .collect(Collectors.toMap(FanDailyAddedResponse::getDate, Function.identity(), (left, right) -> left));

        List<FanDailyAddedResponse> dailyAdded = new ArrayList<>();
        long addedTotal = 0L;
        for (int i = 0; i < days; i++) {
            String date = startDate.plusDays(i).toString();
            FanDailyAddedResponse item = dailyMap.get(date);
            if (item == null) {
                item = new FanDailyAddedResponse();
                item.setDate(date);
                item.setCount(0L);
            }
            addedTotal += item.getCount() == null ? 0L : item.getCount();
            dailyAdded.add(item);
        }

        FanAnalysisPeriodResponse period = new FanAnalysisPeriodResponse();
        period.setDays(days);
        period.setStartDate(startDate.toString());
        period.setEndDate(endDate.toString());
        period.setAddedTotal(addedTotal);
        period.setDailyAdded(dailyAdded);
        return period;
    }

    /**
     * 同步粉丝列表至redis
     * @param userId 用户id
     */
    private void syncFansList2Redis(Long userId) {
        String redisKey = RedisKeyConstants.USER_FANS_KEY_PREFIX + userId;
        //查询db
        List<Fans> fansList = lambdaQuery()
                .select(Fans::getFansUserId, Fans::getCreateTime)
                .eq(Fans::getUserId, userId)
                .last("LIMIT 5000")
                .list();
        if (CollUtil.isNotEmpty(fansList)) {
            //构建luaArgs
            long ttl =  60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            Object[] luaArgs = buildFansLuaArgs(fansList, ttl);
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/follow_batch_add_and_expire.lua")));
            redisScript.setResultType(Long.class);
            try {
                redisTemplate.execute(redisScript, Collections.singletonList(redisKey), luaArgs);
                log.info("========> 用户 [{}] 粉丝列表同步成功", userId);
            } catch (Exception e) {
                log.error("========> 同步失败, result: ", e);
            }
        }
    }

    /**
     * 构建脚本参数
     * @param fansList 粉丝列表
     * @param ttl 过期时间
     * @return 参数
     */
    private Object[] buildFansLuaArgs(List<Fans> fansList, long ttl) {
        int argLength = fansList.size() * 2 + 1;
        int i = 0;
        Object[] args = new Object[argLength];
        for (Fans fan : fansList) {
            args[i] = DateUtil.localDateTime2TimeStamp(fan.getCreateTime());
            args[i+1] = fan.getFansUserId();
            i += 2;
        }
        args[argLength - 1] =ttl;
        return args;
    }
}
