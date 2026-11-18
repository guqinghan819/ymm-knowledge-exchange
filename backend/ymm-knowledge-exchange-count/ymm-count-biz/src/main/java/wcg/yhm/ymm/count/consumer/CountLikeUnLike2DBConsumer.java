package wcg.yhm.ymm.count.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.checkerframework.checker.index.qual.SameLen;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.dto.AggregationCountLikeDTO;
import wcg.yhm.ymm.count.domain.po.NoteCount;
import wcg.yhm.ymm.count.domain.po.UserCount;
import wcg.yhm.ymm.count.mapper.NoteCountMapper;
import wcg.yhm.ymm.count.mapper.UserCountMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-11
 * @Description: 点赞计数落库消费者
 * @Version: 1.0
 */

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_COUNT_NOTE_2_DB,
topic = MQConstants.TOPIC_COUNT_NOTE_2_DB)
public class CountLikeUnLike2DBConsumer implements RocketMQListener<String> {
    @Resource
    private NoteCountMapper noteCountMapper;
    @Resource
    private UserCountMapper userCountMapper;
    private RateLimiter rateLimiter = RateLimiter.create(5000);
    @Override
    public void onMessage(String s) {
        // 对批量回写限速，先更新笔记获赞数，再按作者聚合更新用户获赞总数。
        rateLimiter.acquire();
        List<AggregationCountLikeDTO> countList = null;
        try {
            countList = JsonUtil.str2List(s, AggregationCountLikeDTO.class);
        } catch (Exception e) {
            log.error("## 数据转换成 countList 失败, error: ", e);
        }
        if (CollUtil.isNotEmpty(countList)) {
            List<NoteCount> noteCounts = countList.stream().map(c -> {
                NoteCount noteCount = new NoteCount();
                noteCount.setLikeTotal(c.getCount());
                noteCount.setNoteId(c.getNoteId());
                return noteCount;
            }).toList();
            noteCountMapper.saveOrUpdateNoteLikeBatch(noteCounts);
            log.info("=======> 笔记维度获赞计数落库成功");

            //按照创作者id进行分组
            Map<Long, List<AggregationCountLikeDTO>> map =
                    countList.stream().collect(Collectors.groupingBy(AggregationCountLikeDTO::getCreatorId));
            //统计每个创作者发布笔记的获赞
            List<UserCount> userCounts = new ArrayList<>();
            map.forEach((k, v) -> {
                UserCount userCount = new UserCount();
                userCount.setUserId(k);
                long count = 0;
                for (AggregationCountLikeDTO dto : v) {
                    count += dto.getCount();
                }
                userCount.setLikeTotal(count);
                userCounts.add(userCount);
            });
            userCountMapper.saveOrUpdateLikeTotalBatch(userCounts);
            log.info("=======> 用户维度获赞计数落库成功");
        }

    }
}
    
