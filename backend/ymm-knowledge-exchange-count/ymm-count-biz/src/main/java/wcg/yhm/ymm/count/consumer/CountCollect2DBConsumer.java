package wcg.yhm.ymm.count.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.constant.MQConstants;
import wcg.yhm.ymm.count.domain.dto.AggregationCountCollectDTO;
import wcg.yhm.ymm.count.domain.po.NoteCount;
import wcg.yhm.ymm.count.domain.po.UserCount;
import wcg.yhm.ymm.count.mapper.NoteCountMapper;
import wcg.yhm.ymm.count.mapper.UserCountMapper;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-12
 * @Description:
 * @Version: 1.0
 */

@Component
@Slf4j
@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB,
topic = MQConstants.TOPIC_COUNT_NOTE_COLLECT_2_DB)
public class CountCollect2DBConsumer implements RocketMQListener<String> {
    @Resource
    private NoteCountMapper noteCountMapper;
    @Resource
    private UserCountMapper userCountMapper;
    @Override
    public void onMessage(String s) {
        List<AggregationCountCollectDTO> countList = null;
        try {
            countList = JsonUtil.str2List(s, AggregationCountCollectDTO.class);
        } catch (Exception e) {
            log.error("## 数据转换成 countList 失败, error: ", e);
        }
        if (CollUtil.isNotEmpty(countList)) {
            //笔记维度收藏
            List<NoteCount> noteCounts = countList.stream().map(c -> {
                NoteCount noteCount = new NoteCount();
                noteCount.setNoteId(c.getNoteId());
                noteCount.setCollectTotal(c.getCount());
                return noteCount;
            }).toList();
            noteCountMapper.saveOrUpdateNoteCollectBatch(noteCounts);
            log.info("=======> 笔记维度收藏计数落库成功");

            //用户维度收藏
            //按创作者id分组
            Map<Long, List<AggregationCountCollectDTO>> map =
                    countList.stream().collect(Collectors.groupingBy(AggregationCountCollectDTO::getCreatorId));
            List<UserCount> userCounts = new ArrayList<>();
            map.forEach((k, v) -> {
                UserCount userCount = new UserCount();
                userCount.setUserId(k);
                long count = 0;
                for (AggregationCountCollectDTO dto : v) {
                    count += dto.getCount();
                }
                userCount.setCollectTotal(count);
                userCounts.add(userCount);
            });
            userCountMapper.saveOrUpdateNoteCollectBatch(userCounts);
            log.info("=======> 用户维度收藏计数落库成功");
        }
    }
}
    
