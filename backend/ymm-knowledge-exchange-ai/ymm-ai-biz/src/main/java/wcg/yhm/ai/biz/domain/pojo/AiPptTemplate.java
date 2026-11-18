package wcg.yhm.ai.biz.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * AI PPT模板表
 * </p>
 *
 * @author 雨好绵
 * @since 2026-05-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_ppt_template")
public class AiPptTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板唯一编码
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板说明
     */
    private String templateDesc;

    /**
     * 模板结构JSON
     */
    private String templateSchema;

    /**
     * PPT模板文件路径
     */
    private String filePath;

    /**
     * 风格标签：科技,商务,简约
     */
    private String styleTags;

    /**
     * 模板页数
     */
    private Integer slideCount;

    /** 模板创建时间 */
    private LocalDateTime createTime;


}
