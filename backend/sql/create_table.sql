-- 创建库
create database if not exists ymm_knowledge_exchange;
use ymm_knowledge_exchange;

-- 用户表 b'0' 表示0二进制位
create table if not exists user (
    id bigint unsigned not null auto_increment comment '主键id',
    ymm_id varchar(15) not null comment '平台唯一凭证(全局唯一)',
    nickname varchar(24) not null comment '昵称',
    password varchar(64) default null comment '密码',
    birthday date default null comment '生日',
    avatar varchar(120) default null comment '头像',
    introduce varchar(512) default null comment '个性签名',
    sex tinyint default 0 comment '性别(男-0, 女-1)',
    background_img varchar(120) default null comment '背景图片',
    phone varchar(11) not null comment '手机号',
    status tinyint default 0 comment '账号状态(0-启用, 1-禁用)',
    create_time datetime not null default CURRENT_TIMESTAMP comment '创建时间',
    update_time datetime not null default CURRENT_TIMESTAMP comment '修改时间',
    is_delete bit(1) not null default b'0' comment '逻辑删除(0-存在, 1-已删)',
    primary key (id) USING BTREE,
    UNIQUE KEY uk_ymm_id(ymm_id),
    UNIQUE KEY uk_phone(phone),
    index idx_nickname(nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE `role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名',
  `role_key` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色唯一标识',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0：启用 1：禁用)',
  `sort` int unsigned NOT NULL DEFAULT 0 COMMENT '管理系统中的显示顺序',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后一次更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';


-- 权限表
CREATE TABLE `permission` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
    `name` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
    `type` tinyint unsigned NOT NULL COMMENT '类型(1：目录 2：菜单 3：按钮)',
    `menu_url` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单路由',
    `menu_icon` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单图标',
    `sort` int unsigned NOT NULL DEFAULT 0 COMMENT '管理系统中的显示顺序',
    `permission_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限标识',
    `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态(0：启用；1：禁用)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

--  用户-角色关系表
CREATE TABLE `user_role_rel` (
   `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
   `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
   `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
   `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
   PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色表';


-- 权限-角色关系表
CREATE TABLE `role_permission_rel` (
 `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
 `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
 `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
 `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
 PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权限表';


-- 测试数据
INSERT INTO `permission` (`id`, `parent_id`, `name`, `type`, `menu_url`, `menu_icon`, `sort`, `permission_key`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES (1, 0, '发布笔记', 3, '', '', 1, 'app:note:publish', 0, now(), now(), b'0');
INSERT INTO `permission` (`id`, `parent_id`, `name`, `type`, `menu_url`, `menu_icon`, `sort`, `permission_key`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES (2, 0, '发布评论', 3, '', '', 2, 'app:comment:publish', 0, now(), now(), b'0');

INSERT INTO `role` (`id`, `role_name`, `role_key`, `status`, `sort`, `remark`, `create_time`, `update_time`, `is_deleted`) VALUES (1, '普通用户', 'common_user', 0, 1, '', now(), now(), b'0');

INSERT INTO `role_permission_rel` (`id`, `role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`) VALUES (1, 1, 1, now(), now(), b'0');
INSERT INTO `role_permission_rel` (`id`, `role_id`, `permission_id`, `create_time`, `update_time`, `is_deleted`) VALUES (2, 1, 2, now(), now(), b'0');


-- 频道表
CREATE TABLE `channel` (
`id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`name` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '频道名称',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
`is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道表';

-- 话题表
CREATE TABLE `topic` (
`id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`name` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '话题名称',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
`is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';


-- 频道-话题关系表
CREATE TABLE `channel_topic_rel` (
`id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`channel_id` bigint(11) unsigned NOT NULL COMMENT '频道ID',
`topic_id` bigint(11) unsigned NOT NULL COMMENT '话题ID',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道-话题关联表';

-- 笔记表
CREATE TABLE `note` (
`id` bigint(11) unsigned NOT NULL COMMENT '主键ID',
`title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
`is_content_empty` bit(1) NOT NULL DEFAULT b'0' COMMENT '内容是否为空(0：不为空 1：空)',
`creator_id` bigint(11) unsigned NOT NULL COMMENT '发布者ID',
`topic_id` bigint(11) unsigned DEFAULT NULL COMMENT '话题ID',
`topic_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '话题名称',
`is_top` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否置顶(0：未置顶 1：置顶)',
`type` tinyint(2) DEFAULT '0' COMMENT '类型(0：图文 1：视频)',
`img_uris` varchar(660) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '笔记图片链接(逗号隔开)',
`video_uri` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频链接',
`visible` tinyint(2) DEFAULT '0' COMMENT '可见范围(0：公开,所有人可见 1：仅对自己可见)',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
`status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '状态(0：待审核 1：正常展示 2：被删除(逻辑删除) 3：被下架)',
PRIMARY KEY (`id`) USING BTREE,
KEY `idx_creator_id` (`creator_id`),
KEY `idx_topic_id` (`topic_id`),
KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

ALTER table note add column `content_uuid` varchar(36) DEFAULT '' COMMENT '笔记内容UUID';


INSERT INTO `ymm_knowledge_exchange`.`channel` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('科技', now(), now(), 0);
INSERT INTO `ymm_knowledge_exchange`.`channel` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('生活', now(), now(), 0);


INSERT INTO `ymm_knowledge_exchange`.`topic` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('科技知识推荐', now(), now(), 0);
INSERT INTO `ymm_knowledge_exchange`.`topic` (`name`, `create_time`, `update_time`, `is_deleted`) VALUES ('做饭知识推荐', now(), now(), 0);


INSERT INTO `ymm_knowledge_exchange`.`channel_topic_rel` (`channel_id`, `topic_id`, `create_time`, `update_time`) VALUES (1, 1, now(), now());
INSERT INTO `ymm_knowledge_exchange`.`channel_topic_rel` (`channel_id`, `topic_id`, `create_time`, `update_time`) VALUES (2, 2, now(), now());

-- 用户关注表
CREATE TABLE `following` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`user_id` bigint unsigned NOT NULL COMMENT '用户ID',
`following_user_id` bigint unsigned NOT NULL COMMENT '关注的用户ID',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (`id`) USING BTREE,
KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- 粉丝表
CREATE TABLE `fans` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`user_id` bigint unsigned NOT NULL COMMENT '用户ID',
`fans_user_id` bigint unsigned NOT NULL COMMENT '粉丝的用户ID',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (`id`) USING BTREE,
KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户粉丝表';


-- 保证接口幂等性 添加联合索引
ALTER TABLE following ADD UNIQUE uk_user_id_following_user_id(user_id, following_user_id);
-- 因为最左匹配原则，可以删除之前的单个索引
DROP INDEX idx_user_id ON following;

-- 粉丝表也进行操作
ALTER TABLE fans ADD UNIQUE uk_user_id_fans_user_id(user_id, fans_user_id);
DROP INDEX idx_user_id ON fans;
ALTER TABLE fans ADD INDEX idx_user_id_create_time(user_id, create_time);

-- 点赞记录表
CREATE TABLE `note_like` (
    `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint(11) NOT NULL COMMENT '用户ID',
    `note_id` bigint(11) NOT NULL COMMENT '笔记ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '点赞状态(0：取消点赞 1：点赞)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_user_id_note_id` (`user_id`,`note_id`),
    KEY `idx_note_like_user_status_time_note` (`user_id`,`status`,`create_time`,`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记点赞表';

-- 笔记收藏表
CREATE TABLE `note_collection` (
 `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
 `user_id` bigint(11) NOT NULL COMMENT '用户ID',
 `note_id` bigint(11) NOT NULL COMMENT '笔记ID',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '收藏状态(0：取消收藏 1：收藏)',
 PRIMARY KEY (`id`) USING BTREE,
 UNIQUE KEY `uk_user_id_note_id` (`user_id`,`note_id`),
 KEY `idx_note_collection_user_status_time_note` (`user_id`,`status`,`create_time`,`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记收藏表';

-- 笔记计数表
CREATE TABLE `note_count` (
`id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`note_id` bigint(11) unsigned NOT NULL COMMENT '笔记ID',
`like_total` bigint(11) DEFAULT '0' COMMENT '获得点赞总数',
`collect_total` bigint(11) DEFAULT '0' COMMENT '获得收藏总数',
`comment_total` bigint(11) DEFAULT '0' COMMENT '被评论总数',
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `uk_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记计数表';

-- 用户计数表
CREATE TABLE `user_count` (
`id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`user_id` bigint(11) unsigned NOT NULL COMMENT '用户ID',
`fans_total` bigint(11) DEFAULT '0' COMMENT '粉丝总数',
`following_total` bigint(11) DEFAULT '0' COMMENT '关注总数',
`note_total` bigint(11) DEFAULT '0' COMMENT '发布笔记总数',
`like_total` bigint(11) DEFAULT '0' COMMENT '获得点赞总数',
`collect_total` bigint(11) DEFAULT '0' COMMENT '获得收藏总数',
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户计数表';


ALTER TABLE channel_topic_rel ADD INDEX idx_channel_topic_id(channel_id, topic_id);

-- 评论表
CREATE TABLE `comment` (
 `id` bigint (20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
 `note_id` bigint (20) unsigned NOT NULL COMMENT '关联的笔记ID',
 `user_id` bigint (20) unsigned NOT NULL COMMENT '发布者用户ID',
 `content_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '评论内容UUID',
 `is_content_empty` bit(1) NOT NULL DEFAULT b'0' COMMENT '内容是否为空(0：不为空 1：为空)',
 `image_url` varchar(60) NOT NULL DEFAULT '' COMMENT '评论附加图片URL',
 `level` tinyint (2) NOT NULL DEFAULT '1' COMMENT '级别(1：一级评论 2：二级评论)',
 `reply_total` bigint (20) unsigned DEFAULT 0 COMMENT '评论被回复次数，仅一级评论需要',
 `like_total` bigint (20) DEFAULT 0 COMMENT '评论被点赞次数',
 `parent_id` bigint (20) unsigned DEFAULT 0 COMMENT '父ID (若是对笔记的评论，则此字段存储笔记ID; 若是二级评论，则此字段存储一级评论的ID)',
 `reply_comment_id` bigint (20) unsigned DEFAULT 0 COMMENT '回复哪个的评论 (0表示是对笔记的评论，若是对他人评论的回复，则存储回复评论的ID)',
 `reply_user_id` bigint (20) unsigned DEFAULT 0 COMMENT '回复的哪个用户, 存储用户ID',
 `is_top` tinyint (2) NOT NULL DEFAULT '0' COMMENT '是否置顶(0：不置顶 1：置顶)',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
 PRIMARY KEY (`id`) USING BTREE,
 KEY `idx_note_id` (`note_id`) USING BTREE,
 KEY `idx_user_id` (`user_id`) USING BTREE,
 KEY `idx_parent_id` (`parent_id`) USING BTREE,
 KEY `idx_create_time` (`create_time`) USING BTREE,
 KEY `idx_reply_comment_id` (`reply_comment_id`) USING BTREE,
 KEY `idx_reply_user_id` (`reply_user_id`) USING BTREE
) ENGINE = InnoDB COMMENT = '评论表';


-- 评论点赞表
CREATE TABLE `comment_like` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`user_id` bigint NOT NULL COMMENT '用户ID',
`comment_id` bigint NOT NULL COMMENT '评论ID',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
PRIMARY KEY (`id`),
UNIQUE KEY `uk_user_id_comment_id` (`user_id`,`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

ALTER TABLE comment_like ADD COLUMN `status` tinyint NOT NULL DEFAULT '1' COMMENT '点赞状态(0：取消点赞 1：点赞)';

alter table comment add column `child_comment_total` bigint(20) unsigned DEFAULT '0' COMMENT '二级评论总数（只有一级评论才需要统计）';

ALTER TABLE comment ADD COLUMN heat DECIMAL(10, 2) DEFAULT 0 COMMENT '评论热度';


alter table comment add column first_reply_comment_id bigint(20) unsigned default 0 COMMENT '最早回复的评论ID (只有一级评论需要)';

ALTER TABLE comment ADD INDEX idx_comment_one_heat(note_id, `level`, parent_id, heat, id);

ALTER TABLE comment ADD INDEX idx_comment_reply_cursor(note_id, `level`, parent_id, id);




-- RAG笔记父分片表：用于召回后的上下文拼接
CREATE TABLE `ai_note_rag_parent_chunk`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `note_id`        bigint       NOT NULL COMMENT '笔记ID',
    `parent_id`      varchar(64)  NOT NULL COMMENT '父分片唯一ID',
    `generation_id`  varchar(64)  NOT NULL COMMENT '索引代次ID',
    `active`         tinyint      NOT NULL DEFAULT 0 COMMENT '是否为当前有效代次：0否 1是',
    `parent_index`   int          NOT NULL COMMENT '父分片序号',
    `title`          varchar(128) NULL COMMENT '笔记标题',
    `topic_id`       bigint       NULL COMMENT '话题ID',
    `topic_name`     varchar(64)  NULL COMMENT '话题名称',
    `creator_id`     bigint       NULL COMMENT '作者ID',
    `creator_name`   varchar(64)  NULL COMMENT '作者昵称',
    `creator_avatar` varchar(512) NULL COMMENT '作者头像',
    `cover`          varchar(512) NULL COMMENT '笔记封面',
    `type`           tinyint      NULL COMMENT '笔记类型',
    `visible`        tinyint      NULL COMMENT '可见范围',
    `status`         tinyint      NULL COMMENT '笔记状态',
    `content_uuid`   varchar(64)  NULL COMMENT '正文存储UUID',
    `content_hash`   char(64)     NOT NULL COMMENT '清洗后正文hash',
    `parent_hash`    char(64)     NOT NULL COMMENT '父分片hash',
    `parent_start`   int          NOT NULL COMMENT '父分片正文起始位置',
    `parent_end`     int          NOT NULL COMMENT '父分片正文结束位置',
    `parent_size`    int          NOT NULL COMMENT '父分片字数',
    `parent_text`    text         NOT NULL COMMENT '父分片文本',
    `child_count`    int          NOT NULL DEFAULT 0 COMMENT '子分片数量',
    `index_status`   tinyint      NOT NULL DEFAULT 1 COMMENT '索引状态：1已分片 2已向量化 3已删除 4向量化失败',
    `error_message`  varchar(512) NULL COMMENT '失败原因',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_parent_id` (`parent_id`),
    INDEX `idx_note_parent` (`note_id`, `active`, `parent_index`),
    INDEX `idx_note_id` (`note_id`),
    INDEX `idx_content_hash` (`content_hash`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG笔记父分片表';


-- RAG笔记子分片表：后续向量化时每条子分片对应一个向量point
CREATE TABLE `ai_note_rag_child_chunk`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `note_id`       bigint       NOT NULL COMMENT '笔记ID',
    `parent_id`     varchar(64)  NOT NULL COMMENT '父分片唯一ID',
    `generation_id` varchar(64)  NOT NULL COMMENT '索引代次ID',
    `active`        tinyint      NOT NULL DEFAULT 0 COMMENT '是否为当前有效代次：0否 1是',
    `parent_index`  int          NOT NULL COMMENT '父分片序号',
    `point_id`      varchar(64)  NOT NULL COMMENT '向量库point ID',
    `child_index`   int          NOT NULL COMMENT '子分片全局序号',
    `child_hash`    char(64)     NOT NULL COMMENT '子分片hash',
    `child_start`   int          NOT NULL COMMENT '子分片正文起始位置',
    `child_end`     int          NOT NULL COMMENT '子分片正文结束位置',
    `child_size`    int          NOT NULL COMMENT '子分片字数',
    `child_text`    text         NOT NULL COMMENT '子分片文本',
    `snippet`       varchar(800) NULL COMMENT '展示摘要',
    `index_status`  tinyint      NOT NULL DEFAULT 1 COMMENT '索引状态：1已分片 2已向量化 3已删除 4向量化失败',
    `error_message` varchar(512) NULL COMMENT '失败原因',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_point_id` (`point_id`),
    INDEX `idx_note_child` (`note_id`, `active`, `child_index`),
    INDEX `idx_note_id` (`note_id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG笔记子分片表';

-- 笔记状态变更与 RAG 索引同步的事务 Outbox
CREATE TABLE `note_rag_outbox`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `event_id`        varchar(64)  NOT NULL COMMENT '事件唯一ID',
    `note_id`         bigint       NOT NULL COMMENT '笔记ID',
    `event_type`      varchar(16)  NOT NULL COMMENT 'UPSERT/DELETE',
    `note_updated_at` datetime     NULL COMMENT '笔记更新时间',
    `schema_version`  int          NOT NULL DEFAULT 2,
    `status`          tinyint      NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2发送失败',
    `attempts`        int          NOT NULL DEFAULT 0,
    `next_retry_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `error_message`   varchar(512) NULL,
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    INDEX `idx_outbox_pending` (`status`, `next_retry_time`, `id`),
    INDEX `idx_outbox_note` (`note_id`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '笔记RAG事务Outbox';

ALTER TABLE comment_like ADD COLUMN `status` tinyint NOT NULL DEFAULT '1' COMMENT '点赞状态(0：取消点赞 1：点赞)';

ALTER TABLE comment ADD INDEX idx_comment_one_heat(note_id, `level`, parent_id, heat, id);
ALTER TABLE comment ADD INDEX idx_comment_reply_cursor(note_id, `level`, parent_id, id);
