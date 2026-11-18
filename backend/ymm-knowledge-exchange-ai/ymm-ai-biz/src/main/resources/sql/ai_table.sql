CREATE TABLE `ai_conversation`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint       NOT NULL COMMENT '用户ID',
    `conversation_id` varchar(255) NOT NULL COMMENT '会话ID',
    `agent_type`      VARCHAR(255) NULL COMMENT '最后使用的助手类型',
    `title`           varchar(512) NULL COMMENT '会话标题',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_id` (`conversation_id`),
    INDEX `idx_user_id` (`user_id` ASC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'AI对话会话表';


-- AI对话消息表
CREATE TABLE `ai_message`
(
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT NULL COMMENT '用户ID',
    `status`          VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    `error_message`   TEXT NULL,
    `metadata`        LONGTEXT NULL COMMENT '功能附加信息JSON',
    `message_id`      VARCHAR(64)   NOT NULL COMMENT '消息唯一标识',
    `conversation_id` VARCHAR(255)  NOT NULL COMMENT '所属会话ID',
    `question`        LONGTEXT      NOT NULL COMMENT '用户问题',
    `answer`          LONGTEXT      NULL COMMENT '回答内容',
    `created_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `reference`       LONGTEXT      NULL COMMENT '参考链接',
    `agent_type`      VARCHAR(255)  NOT NULL COMMENT '智能体类型',
    `thinking`        LONGTEXT      NULL COMMENT '思考过程',
    `file_id`         VARCHAR(255)  NULL     DEFAULT NULL COMMENT '文件id',
    `recommend`       VARCHAR(1000) NULL     DEFAULT NULL COMMENT '推荐问题',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_user_conversation` (`user_id`, `conversation_id`, `deleted`, `id`),
    INDEX `idx_create_time` (`created_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'AI对话消息表';


ALTER TABLE `ai_conversation`
    DROP INDEX `uk_conversation_id`,
    ADD UNIQUE KEY `uk_user_conversation` (`user_id`, `conversation_id`);


-- ppt 模板
CREATE TABLE `ai_ppt_template`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code`   varchar(50)  NOT NULL COMMENT '模板唯一编码',
    `template_name`   varchar(100) NOT NULL COMMENT '模板名称',
    `template_desc`   text         NULL COMMENT '模板说明',
    `template_schema` longtext     NOT NULL COMMENT '模板结构JSON',
    `file_path`       varchar(500) NOT NULL COMMENT 'PPT模板文件路径',
    `style_tags`      varchar(200) NULL DEFAULT NULL COMMENT '风格标签：科技,商务,简约',
    `slide_count`     int          NULL DEFAULT NULL COMMENT '模板页数',
    `create_time`     datetime     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `template_code` (`template_code` ASC) USING BTREE,
    INDEX `idx_template_code` (`template_code` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI PPT模板表'
  ROW_FORMAT = Dynamic;

CREATE TABLE `ai_ppt_inst`
(
    `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '实例ID',
    `user_id`         BIGINT NULL COMMENT '所属用户ID',
    `conversation_id` varchar(64)   NULL DEFAULT NULL COMMENT '会话ID',
    `template_code`   varchar(50)   NULL DEFAULT NULL COMMENT '选择的模板code',
    `status`          varchar(32)   NULL DEFAULT 'INIT' COMMENT '状态',
    `query`           text          NULL COMMENT '用户原始需求',
    `requirement`     longtext      NULL COMMENT '需求澄清',
    `search_info`     longtext      NULL COMMENT '搜索信息',
    `outline`         longtext      NULL COMMENT '大纲',
    `ppt_schema`      longtext      NULL COMMENT 'AI生成的PPT规划JSON',
    `file_url`        varchar(1000) NULL DEFAULT NULL COMMENT '生成的PPT文件URL',
    `error_msg`       text          NULL COMMENT '失败原因',
    `create_time`     datetime      NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_conversation_id` (`conversation_id` ASC) USING BTREE,
    INDEX `idx_user_conversation` (`user_id`, `conversation_id`),
    INDEX `idx_status` (`status` ASC) USING BTREE,
    INDEX `idx_template_code` (`template_code` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI PPT生成实例表'
  ROW_FORMAT = Dynamic;


