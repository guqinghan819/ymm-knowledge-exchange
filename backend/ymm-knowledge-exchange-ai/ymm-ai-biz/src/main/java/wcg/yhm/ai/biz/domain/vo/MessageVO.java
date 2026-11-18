package wcg.yhm.ai.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-09
 * @Description: 对话消息视图类
 * @Version: 1.0
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageVO {

    /**
     * 消息唯一标识
     */
    private String messageId;

    private String agentType;
    private String status;
    private String errorMessage;
    private java.util.Map<String, Object> metadata;

    /**
     * 用户问题
     */
    private String question;

    /**
     * 回答内容
     */
    private String answer;

    /**
     * 修改时间
     */
    private LocalDateTime updatedTime;

    /**
     * 参考链接
     */
    private String reference;


    /**
     * 思考过程
     */
    private String thinking;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 推荐问题
     */
    private String recommend;
}
    
