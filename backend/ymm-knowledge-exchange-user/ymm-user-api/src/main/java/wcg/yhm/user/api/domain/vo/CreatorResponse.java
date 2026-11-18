package wcg.yhm.user.api.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-24
 * @Description: 创作者大致信息响应
 * @Version: 1.0
 */

@Data
public class CreatorResponse implements Serializable {
    /**
     * 用户id
     */
    private Long id;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;

    private String introduce;
}
    
