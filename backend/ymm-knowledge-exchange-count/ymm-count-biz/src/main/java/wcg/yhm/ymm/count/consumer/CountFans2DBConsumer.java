package wcg.yhm.ymm.count.consumer;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.po.UserCount;
import wcg.yhm.ymm.count.mapper.UserCountMapper;
import wcg.yhm.ymm.count.service.IUserCountService;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-04
 * @Description: 粉丝计数落库消费者
 * @Version: 1.0
 */

@Component
@RocketMQMessageListener(consumerGroup = "ymm_group" + MQConstants.TOPIC_COUNT_FANS_2_DB,
topic = MQConstants.TOPIC_COUNT_FANS_2_DB)
@Slf4j
public class CountFans2DBConsumer implements RocketMQListener<String> {

    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @Resource
    private UserCountMapper userCountMapper;
    @Override
    public void onMessage(String s) {
        rateLimiter.acquire();
        Map<Long, Integer> countMap = null;
        try {
            countMap = JsonUtil.Str2Map(s, Long.class, Integer.class);
        } catch (Exception e) {
            log.error("## 解析 JSON 字符串异常", e);
        }

        if (CollUtil.isNotEmpty(countMap)) {
            List<UserCount> userCounts = countMap.entrySet().stream().map(entry -> {
                Long key = entry.getKey();
                Integer value = entry.getValue();
                UserCount userCount = new UserCount();
                userCount.setUserId(key);
                userCount.setFansTotal(Long.valueOf(value));
                return userCount;
            }).toList();
            userCountMapper.saveOrUpdateFansBatch(userCounts);
            log.info("========> 粉丝计数落库消息消费成功");
        }
    }
}
    
