package wcg.yhm.note.domain.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 笔记表
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("note")
public class Note implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容是否为空(0：不为空 1：空)
     */
    private Boolean isContentEmpty;

    /**
     * 发布者ID
     */
    private Long creatorId;

    /**
     * 话题ID
     */
    private Long topicId;

    /**
     * 话题名称
     */
    private String topicName;

    /**
     * 是否置顶(0：未置顶 1：置顶)
     */
    private Boolean isTop;

    /**
     * 类型(0：图文 1：视频)
     */
    private Integer type;

    /**
     * 笔记图片链接(逗号隔开)
     */
    private String imgUris;

    /**
     * 视频链接
     */
    private String videoUri;

    /**
     * 可见范围(0：公开,所有人可见 1：仅对自己可见)
     */
    private Integer visible;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 状态(0：待审核 1：正常展示 2：被删除(逻辑删除) 3：被下架)
     */
    private Integer status;

    /** 正文在文本存储服务中的内容 ID */
    private String contentUuid;

    /** 最终审核方式：AI 或 MANUAL；待审时为空 */
    private String reviewMethod;
    /** 驳回原因，供笔记作者查看 */
    private String reviewReason;
    /** 最终审核决定的时间 */
    private LocalDateTime reviewedAt;
    /** 内容版本号；编辑递增，防止旧审核结果覆盖新内容 */
    private Integer reviewVersion;
}
