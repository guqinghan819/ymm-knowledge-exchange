//package wcg.yhm.note.consumer;
//
//import cn.hutool.core.bean.BeanUtil;
//import cn.hutool.core.util.StrUtil;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
//import com.google.common.util.concurrent.RateLimiter;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.client.producer.SendCallback;
//import org.apache.rocketmq.client.producer.SendResult;
//import org.apache.rocketmq.common.message.Message;
//import org.apache.rocketmq.spring.annotation.ConsumeMode;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.apache.rocketmq.spring.core.RocketMQTemplate;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.stereotype.Component;
//import wcg.yhm.note.constants.MQConstant;
//import wcg.yhm.note.domain.dataobject.Note;
//import wcg.yhm.note.domain.dataobject.NoteLike;
//import wcg.yhm.note.domain.mapper.NoteLikeMapper;
//import wcg.yhm.note.enums.NoteLikeEnum;
//import wcg.yhm.note.model.dto.MqLikeUnlikeDTO;
//import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;
//
//import java.util.Objects;
//
///**
// * @Author: 雨好绵
// * @CreateTime: 2026-02-10
// * @Description: 点赞、取消点赞消息消费者
// * @Version: 1.0
// */
//
//@RocketMQMessageListener(consumerGroup = "ymm_group_" + MQConstant.TOPIC_NOTE_LIKE_OR_UNLIKE,
//topic = MQConstant.TOPIC_NOTE_LIKE_OR_UNLIKE,
//consumeMode = ConsumeMode.ORDERLY) //设置为顺序消费
//@Component
//@Slf4j
//public class LikeUnlikeConsumer implements RocketMQListener<Message> {
//
//    private RateLimiter rateLimiter = RateLimiter.create(5000);
//    @Resource
//    private NoteLikeMapper noteLikeMapper;
//    @Resource
//    private RocketMQTemplate rocketMQTemplate;
//
//
//    @Override
//    public void onMessage(Message message) {
//        rateLimiter.acquire();
//        String tags = message.getTags();
//        String jsonStr = new String(message.getBody());
//        log.info("==> LikeUnlikeNoteConsumer 消费了消息 {}, tags: {}", jsonStr, tags);
//        // 根据 MQ 标签，判断操作类型
//        if (Objects.equals(tags, MQConstant.TAG_LIKE)) { // 点赞笔记
//            handleLikeNoteTagMessage(jsonStr);
//        } else if (Objects.equals(tags, MQConstant.TAG_UNLIKE)) { // 取消点赞笔记
//            handleUnlikeNoteTagMessage(jsonStr);
//        }
//    }
//
//    /**
//     * 笔记点赞
//     * @param bodyJsonStr
//     */
//    private void handleLikeNoteTagMessage(String bodyJsonStr) {
//        if (StrUtil.isBlank(bodyJsonStr)) return;
//        MqLikeUnlikeDTO mqLikeUnlikeDTO = JsonUtil.parseObject(bodyJsonStr, MqLikeUnlikeDTO.class);
//        NoteLike noteLike = BeanUtil.copyProperties(mqLikeUnlikeDTO, NoteLike.class);
//        noteLike.setStatus(NoteLikeEnum.LIKE.getStatus());
//        boolean result = noteLikeMapper.insertOrUpdate(noteLike);
//        if (result) {
//            log.info("=========> 笔记点赞落库成功");
//            org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();
//            rocketMQTemplate.asyncSend(MQConstant.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {
//                @Override
//                public void onSuccess(SendResult sendResult) {
//                    log.info("========> 点赞计数消息发送成功, result: {}", sendResult);
//                }
//
//                @Override
//                public void onException(Throwable throwable) {
//                    log.error("## 点赞计数消息发送失败, error: ", throwable);
//                }
//            });
//        } else {
//            log.error("## 笔记点赞落库失败");
//        }
//
//    }
//
//    /**
//     * 笔记取消点赞
//     * @param bodyJsonStr
//     */
//    private void handleUnlikeNoteTagMessage(String bodyJsonStr) {
//        MqLikeUnlikeDTO mqLikeUnlikeDTO = JsonUtil.parseObject(bodyJsonStr, MqLikeUnlikeDTO.class);
//        if (Objects.isNull(mqLikeUnlikeDTO)) return;
//        NoteLike noteLike = BeanUtil.copyProperties(mqLikeUnlikeDTO, NoteLike.class);
//        noteLike.setStatus(NoteLikeEnum.UNLIKE.getStatus());
//        int affect = noteLikeMapper.update(new UpdateWrapper<NoteLike>()
//                .eq("user_Id", mqLikeUnlikeDTO.getUserId())
//                .eq("note_id", mqLikeUnlikeDTO.getNoteId())
//                .eq("status", NoteLikeEnum.LIKE.getStatus())
//                .set("status", NoteLikeEnum.UNLIKE.getStatus())
//                .set("create_time", mqLikeUnlikeDTO.getCreateTime()));
//        if (affect > 0) {
//            log.info("=========> 笔记取消点赞落库成功");
//            //发送计数
//            org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(bodyJsonStr).build();
//            rocketMQTemplate.asyncSend(MQConstant.TOPIC_COUNT_NOTE_LIKE, message, new SendCallback() {
//                @Override
//                public void onSuccess(SendResult sendResult) {
//                    log.info("========> 取消点赞计数消息发送成功, result: {}", sendResult);
//                }
//
//                @Override
//                public void onException(Throwable throwable) {
//                    log.error("## 取消点赞计数消息发送失败, error: ", throwable);
//                }
//            });
//        } else {
//            log.error("## 笔记取消点赞落库失败");
//        }
//    }
//
//
//
//}
//
