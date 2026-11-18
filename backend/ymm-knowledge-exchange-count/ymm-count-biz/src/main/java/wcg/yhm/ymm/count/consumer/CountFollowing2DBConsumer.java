package wcg.yhm.ymm.count.consumer;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.dto.CountFollowUnfollowDTO;
import wcg.yhm.ymm.count.enums.CountTypeEnum;
import wcg.yhm.ymm.count.mapper.UserCountMapper;
import wcg.yhm.ymm.count.service.IUserCountService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-04
 * @Description: 关注计数落库消费者
 * @Version: 1.0
 */


@Component
@RocketMQMessageListener(consumerGroup = "ymm_group" + MQConstants.TOPIC_COUNT_FOLLOWING_2_DB,
topic = MQConstants.TOPIC_COUNT_FOLLOWING_2_DB)
@Slf4j
public class CountFollowing2DBConsumer implements RocketMQListener<String> {
    @Resource
    private UserCountMapper userCountMapper;

    private RateLimiter rateLimiter = RateLimiter.create(5000);
    @Override
    public void onMessage(String s) {
        rateLimiter.acquire();
        CountFollowUnfollowDTO dto = JsonUtil.parseObject(s, CountFollowUnfollowDTO.class);
        if (dto == null) {
            return;
        }
        Integer type = dto.getType();
        long count = Objects.equals(type, CountTypeEnum.FOLLOW.getValue()) ? 1 : -1;
        userCountMapper.saveOrUpdateFollowing(dto.getUserId(), count);
        log.info("========> 关注计数落库消息消费成功");
    }
}
    
