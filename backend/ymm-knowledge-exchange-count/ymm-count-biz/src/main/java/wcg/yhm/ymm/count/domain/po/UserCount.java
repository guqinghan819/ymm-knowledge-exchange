package wcg.yhm.ymm.count.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户计数表
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_count")
public class UserCount implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 粉丝总数
     */
    private Long fansTotal;

    /**
     * 关注总数
     */
    private Long followingTotal;

    /**
     * 发布笔记总数
     */
    private Long noteTotal;

    /**
     * 获得点赞总数
     */
    private Long likeTotal;

    /**
     * 获得收藏总数
     */
    private Long collectTotal;


}
