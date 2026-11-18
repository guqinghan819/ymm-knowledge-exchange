package wcg.yhm.user.relation.biz.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import wcg.yhm.user.relation.biz.constant.MQConstants;
import wcg.yhm.user.relation.biz.constant.RedisKeyConstants;
import wcg.yhm.user.relation.biz.domain.dto.CountFollowUnfollowDTO;
import wcg.yhm.user.relation.api.domain.event.FollowUserMQRequest;
import wcg.yhm.user.relation.api.domain.event.UnFollowUserMQRequest;
import wcg.yhm.user.relation.biz.domain.po.Fans;
import wcg.yhm.user.relation.biz.domain.po.Following;
import wcg.yhm.user.relation.biz.enums.CountTypeEnum;
import wcg.yhm.user.relation.biz.mapper.FansMapper;
import wcg.yhm.user.relation.biz.mapper.FollowingMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-01
 * @Description: 关注、取关消费者
 * @Version: 1.0
 */


@Component
@RocketMQMessageListener(consumerGroup = "ymm_group" + MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
consumeMode = ConsumeMode.ORDERLY) //设置为顺序消费
@Slf4j
public class FollowUnFollowConsumer implements RocketMQListener<Message> {
    @Resource
    private FollowingMapper followingMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private FansMapper fansMapper;
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        String jsonStr = new String(message.getBody());
        String tags = message.getTags();
        log.info("==> FollowUnfollowConsumer 消费了消息 {}, tags: {}", jsonStr, tags);

        //根据不同标签进行不同操作
        if (MQConstants.TAG_FOLLOW.equals(tags)) {
            //关注操作
            handleFollow(jsonStr);
        }
        if (MQConstants.TAG_UNFOLLOW.equals(tags)) {
            //取关操作
            handleUnfollow(jsonStr);

        }
    }

    /**
     * 处理取关操作
     * @param jsonStr
     */
    private void handleUnfollow(String jsonStr) {
        UnFollowUserMQRequest unFollowUserMQRequest = JsonUtil.parseObject(jsonStr, UnFollowUserMQRequest.class);
        if (unFollowUserMQRequest == null) return;
        Long unfollowUserId = unFollowUserMQRequest.getUnfollowUserId();
        Long userId = unFollowUserMQRequest.getUserId();
        LocalDateTime createTime = unFollowUserMQRequest.getCreateTime();

        //编程式事务删除db记录
        //删除关注表记录
        boolean result = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                //删除关注表记录
                int count = followingMapper.delete(new LambdaQueryWrapper<Following>()
                        .eq(Following::getUserId, userId)
                        .eq(Following::getFollowingUserId, unfollowUserId));
                if (count > 0) {
                    //删除粉丝表
                    fansMapper.delete(new LambdaQueryWrapper<Fans>()
                            .eq(Fans::getUserId, unfollowUserId)
                            .eq(Fans::getFansUserId, userId));
                }
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                log.error("", e);
            }
            return false;
        }));
        if (result) {
            //从粉丝列表中删除
            String redisKey = RedisKeyConstants.USER_FANS_KEY_PREFIX + unfollowUserId;
            redisTemplate.opsForZSet().remove(redisKey, userId);
            //发送消息进行粉丝计数，关注计数
            CountFollowUnfollowDTO countFollowUnfollowDTO = new CountFollowUnfollowDTO();
            countFollowUnfollowDTO.setType(CountTypeEnum.UNFOLLOW.getValue());
            countFollowUnfollowDTO.setUserId(userId);
            countFollowUnfollowDTO.setTargetUserId(unfollowUserId);
            sendMessage(countFollowUnfollowDTO);
        }
    }

    /**
     * 关注写库处理
     * @param jsonStr json字符串
     */
    private void handleFollow(String jsonStr) {
        FollowUserMQRequest mqRequest = JsonUtil.parseObject(jsonStr, FollowUserMQRequest.class);
        if (mqRequest == null) return;
        //幂等性靠联合索引保障
        Long userId = mqRequest.getUserId();
        Long followUserId = mqRequest.getFollowUserId();
        LocalDateTime createTime = mqRequest.getCreateTime();
        boolean result = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                Following following = new Following();
                following.setUserId(userId);
                following.setCreateTime(createTime);
                following.setFollowingUserId(followUserId);
                int count = followingMapper.insert(following);
                if (count > 0) {
                    //粉丝表插入
                    Fans fans = new Fans();
                    fans.setFansUserId(userId);
                    fans.setUserId(followUserId);
                    fans.setCreateTime(createTime);
                    fansMapper.insert(fans);
                }
                return true;
            } catch (Exception e) {
                //回滚
                status.setRollbackOnly();
                log.error("关注失败，事务回滚", e);
            }
            return false;
        }));
        log.info("## 数据库添加记录结果：{}", result);
        //如果成功
        if (result) {
            //redis更新被关注的的粉丝列表
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/script/follow_check_and_update_fans_zset.lua")));
            redisScript.setResultType(Long.class);
            //时间戳
            long timeStamp = DateUtil.localDateTime2TimeStamp(createTime);
            String redisKey = RedisKeyConstants.USER_FANS_KEY_PREFIX + followUserId;
            Long success = redisTemplate.execute(redisScript, Collections.singletonList(redisKey), userId, timeStamp);
            log.info("============> success: {}", success);
            //发送消息进行粉丝计数，关注计数
            CountFollowUnfollowDTO countFollowUnfollowDTO = new CountFollowUnfollowDTO();
            countFollowUnfollowDTO.setType(CountTypeEnum.FOLLOW.getValue());
            countFollowUnfollowDTO.setUserId(userId);
            countFollowUnfollowDTO.setTargetUserId(followUserId);
            sendMessage(countFollowUnfollowDTO);
        }
    }

    /**
     * 发送消息
     * @param countFollowUnfollowDTO
     */
    private void sendMessage(CountFollowUnfollowDTO countFollowUnfollowDTO) {
        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload(JsonUtil.toJsonString(countFollowUnfollowDTO)).build();
        //关注计数
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=========> 关注计数消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("========> 关注计数消息发送失败", throwable);
            }
        });

        //粉丝计数
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=========> 粉丝计数消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("========> 粉丝计数消息发送失败", throwable);
            }
        });
    }
}
    
