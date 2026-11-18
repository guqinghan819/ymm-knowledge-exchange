-- 笔记下架通知：笔记服务事务 Outbox。
CREATE TABLE `note_notification_outbox` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_id` varchar(64) NOT NULL COMMENT '事件唯一ID',
    `event_type` varchar(32) NOT NULL COMMENT '事件类型',
    `note_id` bigint unsigned NOT NULL COMMENT '笔记ID',
    `recipient_id` bigint unsigned NOT NULL COMMENT '通知接收者ID',
    `note_title` varchar(64) NOT NULL DEFAULT '' COMMENT '笔记标题快照',
    `reason_code` varchar(32) NOT NULL COMMENT '下架原因编码',
    `reason` varchar(255) NOT NULL COMMENT '完整下架原因',
    `occurred_at` datetime NOT NULL COMMENT '下架时间',
    `schema_version` int NOT NULL DEFAULT 1 COMMENT '消息契约版本',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2发送失败',
    `attempts` int NOT NULL DEFAULT 0 COMMENT '发送次数',
    `next_retry_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
    `error_message` varchar(512) DEFAULT NULL COMMENT '最近一次错误',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_note_notification_event_id` (`event_id`),
    KEY `idx_note_notification_pending` (`status`, `next_retry_time`, `id`),
    KEY `idx_note_notification_note` (`note_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记通知事务Outbox';

-- 通知服务保存平台通知的展示快照；平台通知没有行为用户。
ALTER TABLE `notification`
    MODIFY COLUMN `actor_id` bigint unsigned DEFAULT NULL COMMENT '行为用户ID，平台通知为空',
    ADD COLUMN `target_title` varchar(64) DEFAULT NULL COMMENT '通知目标标题快照' AFTER `target_id`,
    ADD COLUMN `reason_code` varchar(32) DEFAULT NULL COMMENT '平台处理原因编码' AFTER `target_title`,
    ADD COLUMN `reason` varchar(255) DEFAULT NULL COMMENT '平台处理完整原因' AFTER `reason_code`,
    MODIFY COLUMN `type` tinyint NOT NULL COMMENT '通知类型（1关注 2笔记点赞 3笔记收藏 4笔记下架）';
