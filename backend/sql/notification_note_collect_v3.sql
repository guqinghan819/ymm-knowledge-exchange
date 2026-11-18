ALTER TABLE `notification`
    MODIFY COLUMN `type` tinyint NOT NULL COMMENT '通知类型（1：关注 2：笔记点赞 3：笔记收藏）';
