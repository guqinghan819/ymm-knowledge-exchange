ALTER TABLE `notification`
    ADD COLUMN `target_id` bigint unsigned DEFAULT NULL COMMENT '通知目标ID（点赞通知为笔记ID）' AFTER `type`,
    MODIFY COLUMN `type` tinyint NOT NULL COMMENT '通知类型（1：关注 2：笔记点赞）',
    DROP INDEX `idx_notification_business_state`,
    ADD KEY `idx_notification_business_state`
        (`recipient_id`, `actor_id`, `type`, `target_id`, `status`, `id`);
