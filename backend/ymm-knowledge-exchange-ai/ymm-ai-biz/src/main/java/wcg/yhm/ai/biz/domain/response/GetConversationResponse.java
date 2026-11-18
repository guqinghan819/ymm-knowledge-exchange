package wcg.yhm.ai.biz.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wcg.yhm.ai.biz.domain.vo.ConversationVO;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-05-09
 * @Description:
 * @Version: 1.0
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetConversationResponse {

    private List<ConversationVO> history;
}
    
