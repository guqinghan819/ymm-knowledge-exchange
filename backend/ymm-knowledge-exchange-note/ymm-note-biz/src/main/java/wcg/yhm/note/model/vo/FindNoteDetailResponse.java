package wcg.yhm.note.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-22
 * @Description: 笔记详情响应类
 * @Version: 1.0
 */


@AllArgsConstructor
@Data
@NoArgsConstructor
public class FindNoteDetailResponse implements Serializable {
    private Long id; //笔记id
    private Integer type; //笔记类型
    private String title; //标题
    private String content; //笔记内容
    private List<String> imgUris; //图片连接
    private Long topicId; //关联话题id
    private String topicName; //话题名称
    private Long creatorId; //创作者id
    private String creatorName; //创作者昵称
    private String avatar; //头像
    private String videoUri; //视频地址
    private LocalDateTime updateTime;
    private Integer visible; //是否公开
    private Boolean isTop; //是否置顶
    private Integer status; //审核状态
    private String reviewMethod;
    private String reviewReason;
    private LocalDateTime reviewedAt;
    private Integer reviewVersion;
    private String reviewTaskStatus;
    private String reviewInternalError;
    private Long channelId; //所属频道id
    private String channelName; //所属频道名称

}
    
