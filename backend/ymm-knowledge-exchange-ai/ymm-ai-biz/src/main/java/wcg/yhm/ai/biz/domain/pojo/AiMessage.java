package wcg.yhm.ai.biz.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * AI对话消息表
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_message")
@Builder
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 消息唯一标识
     */
    private String messageId;

    /** 发送问题的用户 ID */
    private Long userId;
    /** 消息生成状态 */
    private String status;
    /** 消息生成失败时的错误信息 */
    private String errorMessage;
    /** 消息扩展元数据，通常保存为 JSON */
    private String metadata;

    /**
     * 所属会话ID
     */
    private String conversationId;

    /**
     * 用户问题
     */
    private String question;

    /**
     * 回答内容
     */
    private String answer;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 修改时间
     */
    private LocalDateTime updatedTime;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 参考链接
     */
    private String reference;

    /**
     * 智能体类型
     */
    private String agentType;

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
