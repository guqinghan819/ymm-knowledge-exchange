ALTER TABLE ai_note_rag_parent_chunk
    ADD COLUMN generation_id varchar(64) NOT NULL DEFAULT '' COMMENT '索引代次ID' AFTER parent_id,
    ADD COLUMN active tinyint NOT NULL DEFAULT 0 COMMENT '是否为当前有效代次：0否 1是' AFTER generation_id,
    ADD INDEX idx_note_generation_active (note_id, generation_id, active);

ALTER TABLE ai_note_rag_child_chunk
    ADD COLUMN generation_id varchar(64) NOT NULL DEFAULT '' COMMENT '索引代次ID' AFTER parent_id,
    ADD COLUMN active tinyint NOT NULL DEFAULT 0 COMMENT '是否为当前有效代次：0否 1是' AFTER generation_id,
    ADD INDEX idx_note_generation_active (note_id, generation_id, active);

CREATE TABLE note_rag_outbox
(
    id               bigint       NOT NULL AUTO_INCREMENT,
    event_id         varchar(64)  NOT NULL COMMENT '事件唯一ID',
    note_id          bigint       NOT NULL COMMENT '笔记ID',
    event_type       varchar(16)  NOT NULL COMMENT 'UPSERT/DELETE',
    note_updated_at  datetime     NULL COMMENT '笔记更新时间',
    schema_version   int          NOT NULL DEFAULT 2,
    status           tinyint      NOT NULL DEFAULT 0 COMMENT '0待发送 1已发送 2发送失败',
    attempts         int          NOT NULL DEFAULT 0,
    next_retry_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message    varchar(512) NULL,
    create_time      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_outbox_pending (status, next_retry_time, id),
    INDEX idx_outbox_note (note_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '笔记RAG事务Outbox';

-- 首次部署时回填所有当前公开正常笔记。消费者会重新读取实时状态和正文，重复执行也是安全的。
INSERT INTO note_rag_outbox
    (event_id, note_id, event_type, note_updated_at, schema_version, status, attempts, next_retry_time,
     create_time, update_time)
SELECT UUID(), id, 'UPSERT', update_time, 2, 0, 0, NOW(), NOW(), NOW()
FROM note
WHERE status = 1
  AND visible = 0;
