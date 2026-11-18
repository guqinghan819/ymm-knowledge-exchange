package wcg.yhm.comment.biz.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 评论表
 * </p>
 *
 * @author 雨好绵
 * @since 2026-04-03
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库主键 */
    private Long id;

    /**
     * 关联的笔记ID
     */
    private Long noteId;

    /**
     * 发布者用户ID
     */
    private Long userId;

    /**
     * 评论内容UUID
     */
    private String contentUuid;

    /**
     * 内容是否为空(0：不为空 1：为空)
     */
    private Boolean isContentEmpty;

    /**
     * 评论附加图片URL
     */
    private String imageUrl;

    /**
     * 级别(1：一级评论 2：二级评论)
     */
    private Integer level;

    /**
     * 评论被回复次数，仅一级评论需要
     */
    private Long replyTotal;

    /**
     * 评论被点赞次数
     */
    private Long likeTotal;

    /**
     * 父ID (若是对笔记的评论，则此字段存储笔记ID; 若是二级评论，则此字段存储一级评论的ID)
     */
    private Long parentId;

    /**
     * 回复哪个的评论 (0表示是对笔记的评论，若是对他人评论的回复，则存储回复评论的ID)
     */
    private Long replyCommentId;

    /**
     * 回复的哪个用户, 存储用户ID
     */
    private Long replyUserId;

    /**
     * 是否置顶(0：不置顶 1：置顶)
     */
    private Integer isTop;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 子评论总数
     */
    private Long childCommentTotal;

    /**
     * 最早回复的子评论id
     */
    private Long firstReplyCommentId;

    /**
     * 评论热度
     */
    private BigDecimal heat;
}
