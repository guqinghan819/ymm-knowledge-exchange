package wcg.yhm.user.relation.biz.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.user.relation.api.domain.request.QueryFollowRequest;
import wcg.yhm.user.relation.api.domain.event.FollowUserMQRequest;
import wcg.yhm.user.relation.api.domain.event.UnFollowUserMQRequest;
import wcg.yhm.user.relation.api.domain.response.FollowResultResponse;
import wcg.yhm.user.relation.biz.constant.MQConstants;
import wcg.yhm.user.relation.biz.constant.RedisKeyConstants;
import wcg.yhm.user.relation.biz.domain.dto.*;
import wcg.yhm.user.relation.biz.domain.po.Following;
import wcg.yhm.user.relation.biz.domain.vo.FindFollowUserResponse;
import wcg.yhm.user.relation.biz.enums.ErrorCodeEnum;
import wcg.yhm.user.relation.biz.enums.LuaResultEnum;
import wcg.yhm.user.relation.biz.mapper.FollowingMapper;
import wcg.yhm.user.relation.biz.rpc.UserRpcService;
import wcg.yhm.user.relation.biz.service.IFollowingService;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户关注表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-01-25
 */
@Service
@Slf4j
public class FollowingServiceImpl extends ServiceImpl<FollowingMapper, Following> implements IFollowingService {
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Override
    public void followUser(FollowingRequest followingRequest) {
        Long followUserId = followingRequest.getFollowUserId();
        Long userId = UserContextHolder.getUserId();
        //判断被关注用户是否是自己
        if (followUserId.equals(userId)) {
            throw new BizException(ErrorCodeEnum.CANT_FOLLOW_YOUR_SELF);
        }
        //调用rpc判断目标关注用户是否存在
        CreatorResponse destUser = userRpcService.getUserById(followUserId);
        if (destUser == null) {
            throw new BizException(ErrorCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }
        String redisKey = RedisKeyConstants.USER_FOLLOWING_KEY_PREFIX + userId;
        // Lua 在 Redis 内原子地检查并新增关注，避免并发请求产生重复关系。
        //Long 指定 将返回结果转换成 指定类型
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();

        //文件读取脚本
        //new ClassPathResource("/script/follow_check_and_add.lua") 去项目的 src/main/resources 目录下寻找文件。
        //new ResourceScriptSource 负责读取资源文件里的内容。
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/follow_check_and_add.lua")));
        //设置返回类型
        redisScript.setResultType(Long.class);
        //将当前时间转换成时间戳
        long timeStamp = DateUtil.localDateTime2TimeStamp(LocalDateTime.now());
        // KEYS[1] Collections.singletonList(redisKey) singletonList更省内存空间 只读且固定长度
        //ARGV[1] ARGV[2] timeStamp, followUserId
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(redisKey), timeStamp, followUserId);
        checkResult(result);


       if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
           //当关注列表不存在 将db中的数据同步至redis中并设置ttl
           //数据库查询
           List<Following> followingList = lambdaQuery().eq(Following::getUserId, userId).list();
           //保底一天 + 随机数
           long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
           if (CollUtil.isEmpty(followingList)) {
               //如果列表为空直接添加并设置ttl
               DefaultRedisScript<Long> redisScript2 = new DefaultRedisScript<>();
               redisScript2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/follow_add_and_expire.lua")));
               redisScript2.setResultType(Long.class);
               //todo 这里可以通过是否是判断大v用户来设置ttl (粉丝数判断)
               redisTemplate.execute(redisScript2, Collections.singletonList(redisKey), timeStamp, followUserId, ttl);
           } else {
               //不为空就将数据同步至redis中，并设置ttl
               DefaultRedisScript<Long> redisScript3 = new DefaultRedisScript<>();
               redisScript3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/follow_batch_add_and_expire.lua")));
               redisScript3.setResultType(Long.class);
               Object[] luaArgs = buildLuaArgs(followingList, ttl);
               redisTemplate.execute(redisScript3, Collections.singletonList(redisKey), luaArgs);
               //将新关注添加
               result = redisTemplate.execute(redisScript, Collections.singletonList(redisKey), timeStamp, followUserId);
               checkResult(result);
           }
       }

        //发送mq进行异步写库
        String operationId = UUID.randomUUID().toString();
        FollowUserMQRequest followUserMQRequest = new FollowUserMQRequest();
        followUserMQRequest.setOperationId(operationId);
        followUserMQRequest.setUserId(userId);
        followUserMQRequest.setFollowUserId(followUserId);
        followUserMQRequest.setCreateTime(LocalDateTime.now());
        //构建消息对象
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(followUserMQRequest))
                .setHeader(RocketMQHeaders.KEYS, operationId)
                .build();
        //用: 进行拼接，发送topic时可携带tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;
        log.info("=======> 开始发送关注操作 MQ, {}", followUserMQRequest);
        // 同一用户使用相同 hashKey，保证关注和取关消息按顺序处理。
        String hashKey = String.valueOf(userId);
        rocketMQTemplate.asyncSendOrderly(destination, message, hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("========> MQ发送成功, SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("========> MQ发送异常: ", throwable);
            }
        });
    }



    @Override
    public void unfollowUser(UnFollowRequest unFollowRequest) {
        Long unfollowUserId = unFollowRequest.getUnfollowUserId();
        Long userId = UserContextHolder.getUserId();

        //校验取关用户是否是自己
        if (Objects.equals(userId, unfollowUserId)) {
            throw new BizException(ErrorCodeEnum.CANT_UNFOLLOW_YOUR_SELF);
        }
        //判断取关用户是否存在
        CreatorResponse unfollowUser = userRpcService.getUserById(unfollowUserId);
        if (unfollowUser == null) {
            throw new BizException(ErrorCodeEnum.FOLLOW_USER_NOT_EXISTED);
        }

        //执行脚本
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/unfollow_check_and_delete.lua")));
        redisScript.setResultType(Long.class);
        String redisKey = RedisKeyConstants.USER_FOLLOWING_KEY_PREFIX + userId;
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(redisKey), unfollowUserId);

        //如果列表不存在，查询db
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            //查询db
            List<Following> followingList = lambdaQuery().eq(Following::getUserId, userId).list();
            if (CollUtil.isEmpty(followingList)) {
                throw new BizException(ErrorCodeEnum.NOT_FOLLOWED);
            }
            //将数据全部同步到redis中
            long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
            Object[] luaArgs = buildLuaArgs(followingList, ttl);
            //执行同步脚本
            DefaultRedisScript<Long> redisScript3 = new DefaultRedisScript<>();
            redisScript3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/follow_batch_add_and_expire.lua")));
            redisScript3.setResultType(Long.class);
            redisTemplate.execute(redisScript3, Collections.singletonList(redisKey), luaArgs);
            //再次执行删除
            result = redisTemplate.execute(redisScript, Collections.singletonList(redisKey), unfollowUserId);
            checkResult(result);
        }


        //发送mq
        String operationId = UUID.randomUUID().toString();
        UnFollowUserMQRequest unFollowUserMQRequest = new UnFollowUserMQRequest();
        unFollowUserMQRequest.setOperationId(operationId);
        unFollowUserMQRequest.setUnfollowUserId(unfollowUserId);
        unFollowUserMQRequest.setUserId(userId);
        unFollowUserMQRequest.setCreateTime(LocalDateTime.now());

        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(unFollowUserMQRequest))
                .setHeader(RocketMQHeaders.KEYS, operationId)
                .build();
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_UNFOLLOW;
        String hashKey = String.valueOf(userId);
        rocketMQTemplate.asyncSendOrderly(destination, message,hashKey, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=======> 取关消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("=======> 取关消息发送异常", throwable);
            }
        });
    }

    @Override
    public PageResponse<FindFollowUserResponse> findFollowingList(FindFollowUserRequest findFollowUserRequest) {
        //查询页码
        Integer pageNo = findFollowUserRequest.getPageNo();
        //查询目标的id
        Long userId = findFollowUserRequest.getUserId();
        List<FindFollowUserResponse> responseList = null;
        long pageSize = 10; //每页10条数据
                //查询redis中的数据
        String redisKey = RedisKeyConstants.USER_FOLLOWING_KEY_PREFIX + userId;
        Long total = redisTemplate.opsForZSet().zCard(redisKey);
        //判断是否有数据
        if (total != null && total > 0) {
            //计算总页数
            long totalPage = PageResponse.getTotalPage(total, pageSize);
            if (pageNo > totalPage) {
                throw new BizException(ErrorCodeEnum.ERROR_PAGE_NO);
            }
            //从redis zSet中查询分页，设置偏移量(跳过的记录)
            long offset = PageResponse.getOffset(pageNo, pageSize);
            //将set按照score进行降序排序然后进行分页
            //Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY 负无穷到正无穷 保证检索到set中的每个成员
            Set<Object> followUserIdsSet = redisTemplate.opsForZSet()
                    .reverseRangeByScore(redisKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, offset, pageSize);
            if (CollUtil.isNotEmpty(followUserIdsSet)) {
                List<Long> userIds = followUserIdsSet.stream().map(u -> Long.valueOf(u.toString())).toList();
                //批量查询用户
                List<FindUserResponse> followList = userRpcService.findByIds(userIds);
                if (CollUtil.isNotEmpty(followList)) {
                    responseList = followList.stream()
                            .map(u -> BeanUtil.copyProperties(u, FindFollowUserResponse.class))
                            .toList();
                }
            }
        } else {
            //查询db
            Page<Following> followingPage = new Page<>(pageNo, pageSize);
            Page<Following> page = lambdaQuery().eq(Following::getUserId, userId)
                    .orderByDesc(Following::getCreateTime)
                    .page(followingPage);
            List<Following> records = page.getRecords();
            total = page.getTotal();
            if (CollUtil.isNotEmpty(records)) {
                //提取关注用户id
                List<Long> followIds = records.stream().map(Following::getFollowingUserId).toList();
                //查询关注用户信息
                List<FindUserResponse> followUserList = userRpcService.findByIds(followIds);
                if (CollUtil.isNotEmpty(followUserList)) {
                    //进行转换
                    responseList = followUserList.stream()
                            .map(u -> BeanUtil.copyProperties(u, FindFollowUserResponse.class))
                            .toList();

                    //将1000个关注用户异步同步到redis中
                    threadPoolTaskExecutor.submit(() -> syncFollowUserId2Redis(userId));

                }
            }


        }
        return PageResponse.success(responseList, pageNo, total);
    }

    @Override
    public List<FollowResultResponse> checkFollow(QueryFollowRequest queryFollowRequest) {
        Long loginUserId = queryFollowRequest.getLoginUserId();
        List<Long> creatorIds = queryFollowRequest.getCreatorId();
        List<FollowResultResponse> followResultList = new ArrayList<>();
        List<Following> followingList = lambdaQuery().select(Following::getUserId, Following::getFollowingUserId)
                .eq(Following::getUserId, loginUserId)
                .in(Following::getFollowingUserId, creatorIds).list();

       if (CollUtil.isNotEmpty(followingList)) {
           //转换成set(已经关注了的作者id)
           Set<Long> creatorIdsSet = followingList.stream().map(Following::getFollowingUserId).collect(Collectors.toSet());
           //封装
           creatorIds.forEach(creatorId -> {
               FollowResultResponse followResultResponse = new FollowResultResponse();
               followResultResponse.setCreatorId(creatorId);
               if (creatorIdsSet.contains(creatorId)) {
                   //如果set中包含说明已经关注
                   followResultResponse.setFollow(true);
               }
               followResultList.add(followResultResponse);
           });
           return followResultList;
       }
        return List.of();
    }

    /**
     * 将查询的信息同步到redis中
     * @param userId 目标用户id
     */
    private void syncFollowUserId2Redis(Long userId) {
        log.info("================> 关注列表开始同步");
        List<Following> followingList = lambdaQuery()
                .select(Following::getFollowingUserId, Following::getCreateTime)
                .eq(Following::getUserId, userId)
                .last("LIMIT 1000") //查询前1000
                .list();
        String redisKey = RedisKeyConstants.USER_FOLLOWING_KEY_PREFIX + userId;
        long ttl = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
        Object[] luaArgs = buildLuaArgs(followingList, ttl);
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/follow_batch_add_and_expire.lua")));
        redisScript.setResultType(Long.class);
        try {
            redisTemplate.execute(redisScript, Collections.singletonList(redisKey), luaArgs);
            log.info("========> 用户 [{}] 关系列表同步成功", userId);
        } catch (Exception e) {
            log.error("========> 同步失败, result: ", e);
        }

    }

    /**
     * 构建lua参数
     * @param followingList 关注列表
     * @return 参数
     */
    private Object[] buildLuaArgs(List<Following> followingList, long ttl) {
        //时间戳，关注用户，最后一个过期时间
        int argsLength = followingList.size() * 2 + 1;
        Object[] luaArgs = new Object[argsLength];
        int i = 0;
        for (Following following : followingList) {
            luaArgs[i] = DateUtil.localDateTime2TimeStamp(following.getCreateTime()); //时间戳
            luaArgs[i+1] = following.getFollowingUserId(); //关注用户
            i += 2;
        }
        //过期时间
        luaArgs[argsLength - 1] = ttl;
        return luaArgs;
    }

    private void checkResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);
        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回结果错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            case FOLLOW_LIMIT -> throw new BizException(ErrorCodeEnum.FOLLOWING_COUNT_LIMIT);
            case ALREADY_FOLLOWED -> throw new BizException(ErrorCodeEnum.ALREADY_FOLLOWED);
            case NOT_FOLLOWED -> throw new BizException(ErrorCodeEnum.NOT_FOLLOWED);
        }
    }
}
