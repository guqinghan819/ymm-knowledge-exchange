package wcg.yhm.ai.biz.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import wcg.yhm.ai.biz.agent.enums.pptx.PptInstanceStatus;

/**
 * <p>
 * AI PPT生成实例表
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_ppt_inst")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPptInst implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 选择的模板code
     */
    private String templateCode;

    /**
     * 状态
     */
    private String status;

    /**
     * 用户原始需求
     */
    private String query;

    /**
     * 需求澄清
     */
    private String requirement;

    /**
     * 搜索信息
     */
    private String searchInfo;

    /**
     * 大纲
     */
    private String outline;

    /**
     * AI生成的PPT规划JSON
     */
    private String pptSchema;

    /**
     * 生成的PPT文件URL
     */
    private String fileUrl;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 失败原因
     */
    private String errorMsg;

    /** PPT 生成任务创建时间 */
    private LocalDateTime createTime;

    /** PPT 生成任务最后更新时间 */
    private LocalDateTime updateTime;


    /**
     * 获取状态枚举
     */
    public PptInstanceStatus getStatusEnum() {
        return PptInstanceStatus.fromCode(status);
    }

    /**
     * 设置状态枚举
     */
    public void setStatusEnum(PptInstanceStatus statusEnum) {
        this.status = statusEnum != null ? statusEnum.getCode() : null;
    }


}
