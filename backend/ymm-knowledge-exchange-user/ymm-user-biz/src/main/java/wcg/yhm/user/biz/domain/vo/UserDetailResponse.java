package wcg.yhm.user.biz.domain.vo;

import lombok.Data;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-14
 * @Description: 用户信息详情响应类
 * @Version: 1.0
 */

@Data
public class UserDetailResponse {

    private String nickname;

    private String ymmId;

    private String avatar;

    private String backgroundImg;

    //0-男 1-女
    private Integer sex;

    private String introduce;

    //关注数
    private Long followingTotal;

    //粉丝数
    private Long fansTotal;

    //获赞数
    private Long likeTotal;

    //获收藏数
    private Long collectTotal;

    //发布笔记总数
    private Long noteTotal;
}
    
