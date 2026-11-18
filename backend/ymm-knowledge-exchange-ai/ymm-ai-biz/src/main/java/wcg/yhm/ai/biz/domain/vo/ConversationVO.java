package wcg.yhm.ai.biz.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-09
 * @Description: 获取会话历史
 * @Version: 1.0
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationVO {

    /**
     * 会话标题
     */
    private String title;

    private String agentType;
    private LocalDateTime updatedTime;

    /**
     * 会话id
     */
    private String conversationId;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
    
