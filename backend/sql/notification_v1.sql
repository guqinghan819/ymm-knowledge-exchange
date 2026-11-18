CREATE TABLE `notification` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `event_id` varchar(64) NOT NULL COMMENT '来源事件ID',
    `recipient_id` bigint unsigned NOT NULL COMMENT '通知接收者ID',
    `actor_id` bigint unsigned NOT NULL COMMENT '行为用户ID',
    `type` tinyint NOT NULL COMMENT '通知类型（1：关注）',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0：已撤回 1：生效）',
    `is_read` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已读',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
    `revoke_time` datetime DEFAULT NULL COMMENT '撤回时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_event_id` (`event_id`),
    KEY `idx_notification_recipient_status_id` (`recipient_id`, `status`, `id`),
    KEY `idx_notification_recipient_status_read` (`recipient_id`, `status`, `is_read`),
    KEY `idx_notification_business_state` (`recipient_id`, `actor_id`, `type`, `status`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';

CREATE TABLE `notification_event_log` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_id` varchar(64) NOT NULL COMMENT '事件ID',
    `event_type` varchar(32) NOT NULL COMMENT '事件类型',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_event_log_event_id` (`event_id`),
    KEY `idx_notification_event_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知事件幂等日志';
