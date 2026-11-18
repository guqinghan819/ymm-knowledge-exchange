package wcg.yhm.comment.biz.retry;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import wcg.yhm.comment.biz.config.properties.RetryProperties;
import wcg.yhm.comment.biz.domain.dto.PublishCommentMqDTO;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-04
 * @Description: 重新发送mq处理
 * @Version: 1.0
 */

@Component
@Slf4j
public class SendMqRetryHelper {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RetryTemplate retryTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * 异步发送消息
     * @param topic 主题名
     * @param body json字符串消息
     */
    public void asyncSend(String topic, String body) {
        log.info("======> 开始异步发送 mq, Topic: {}, body: {}", topic, body);
        Message<String> message = MessageBuilder.withPayload(body).build();

        //异步发送提升接口响应速度
        rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【评论发布】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable e) {
                log.error("==> 【评论发布】MQ 发送异常: ", e);
                handleRetry(topic, message);
            }
        });
    }

    /**
     * 同步发送消息，确认 MQ 接收成功后返回
     * @param topic 主题名
     * @param body json字符串消息
     */
    public SendResult syncSend(String topic, String body) {
        log.info("======> 开始同步发送 mq, Topic: {}, body: {}", topic, body);
        Message<String> message = MessageBuilder.withPayload(body).build();
        try {
            return retryTemplate.execute((RetryCallback<SendResult, RuntimeException>) context -> {
                log.info("===========> 开始同步重试 mq 发送, 当前重试次数: {}, 时间: {}",
                        context.getRetryCount() + 1, LocalDateTime.now());
                return rocketMQTemplate.syncSend(topic, message);
            });
        } catch (Exception e) {
            fallback(e, topic, body);
            throw new RuntimeException("MQ 消息发送失败", e);
        }
    }

    /**
     * 重试处理
     * @param topic 主题
     * @param message 消息
     */
    private void handleRetry(String topic, Message<String> message) {
        //开启一个新线程异步处理
        taskExecutor.submit(() -> {
            try {
                retryTemplate.execute((RetryCallback<Void, RuntimeException>) context -> {
                    log.info("===========> 开始重试 mq 发送, 当前重试次数: {}, 时间: {}",context.getRetryCount() + 1,
                            LocalDateTime.now());
                    rocketMQTemplate.syncSend(topic, message);
                    return null;
                });
            } catch (Exception e) {
                fallback(e, topic, message.getPayload());
            }
        });
    }

    /**
     *兜底方案,将发送失败的mq写入数据库,定时扫表,将消息再次发送,成功就删除记录
     */
    @Recover
    public void fallback(Exception e, String topic, String bodyJson) {
        log.error("## 多次发送失败, 进入兜底策略, Topic: {}, bodyJson: {}",
                topic, bodyJson);

        //todo

    }
}
    
