package wcg.yhm.ai.biz.agent.pptx;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;
import wcg.yhm.ai.biz.agent.enums.pptx.PptIntent;
import wcg.yhm.ai.biz.agent.pptx.assistant.IntentRecognitionAssistant;
import wcg.yhm.ai.biz.agent.result.PptIntentResult;
import wcg.yhm.ai.biz.domain.pojo.AiPptInst;
import wcg.yhm.ai.biz.service.IAiPptInstService;

import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-11
 * @Description: 意图识别器
 * @Version: 1.0
 */

@Slf4j
@Component
public class PptIntentRecognizer {

    @Resource
    private IntentRecognitionAssistant intentRecognitionAssistant;

    @Resource
    private IAiPptInstService aiPptInstService;


    /**
     * 意图识别
     */
    public PptIntentResult recognize(String conversationId, String query) {
        //获取该对话的最新实例
        AiPptInst aiPptInst = aiPptInstService.lambdaQuery()
                .eq(AiPptInst::getConversationId, conversationId)
                .orderByDesc(AiPptInst::getCreateTime)
                .last("limit 1")
                .one();
        if (Objects.isNull(aiPptInst)) {
            //当没有最新数据默认为创建
            log.info("会话中无PPT实例，默认新建");
            return new PptIntentResult(PptIntent.CREATE_PPT, "会话中无PPT实例，默认新建");
        }

        String errorMsg = aiPptInst.getErrorMsg(); //错误消息
        PptInstanceStatus statusEnum = aiPptInst.getStatusEnum(); //实例状态

        //判断是否进行重连
       if (needResume(statusEnum, errorMsg, query)) {
           log.info("检测到断点重连需求: status={}, hasError={}", statusEnum, StringUtils.hasText(errorMsg));
           return new PptIntentResult(PptIntent.RESUME_PPT,
                   "检测到上次执行未完成，从状态 " + statusEnum + " 继续执行");
       }

       //如果是SUCCESS状态就进行意图识别(CREATE 或 MODIFY)
        if (statusEnum == PptInstanceStatus.SUCCESS) {
            return intentRecognitionAssistant.chat(query);
        }

        //对于其他中间状态和FAIL,默认重新创建
        return new PptIntentResult(PptIntent.CREATE_PPT, "状态为 " + statusEnum + "，默认新建");
    }

    /**
     * 判断是否重连
     */
    private boolean needResume(PptInstanceStatus statusEnum, String errorMsg, String query) {
        if (StrUtil.isNotBlank(errorMsg)) {
            //说明上次执行失败,确认重连
            return true;
        }
        //检测用户是否明确表示重连
        String lowerCase = query.toLowerCase();
        String[] retryKeyWords = {"继续", "重试", "resume", "retry", "继续执行", "继续生成"};
        for (String retryKeyWord : retryKeyWords) {
            if (lowerCase.contains(retryKeyWord)) {
                return true;
            }
        }
        //处于中间状态处理，看用户是否有重新创建的意愿
        if (statusEnum != PptInstanceStatus.INIT && statusEnum != PptInstanceStatus.SUCCESS) {
            String[] createKeyWords = {"重新", "重来", "重头开始", "新生成", "create new", "new"};
            for (String createKeyWord : createKeyWords) {
                if (lowerCase.contains(createKeyWord)) {
                    return false;
                }
            }
            return true; //处于中间状态，且用户没有重新生成意愿，默认为继续
        }
        return false;
    }
}
    
