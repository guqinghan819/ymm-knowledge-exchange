-- Apply before deploying the updated note service. Existing notes retain version 1.
ALTER TABLE note
    ADD COLUMN review_method VARCHAR(16) NULL COMMENT 'AI or MANUAL' AFTER content_uuid,
    ADD COLUMN review_reason VARCHAR(200) NULL COMMENT 'Author-visible rejection reason' AFTER review_method,
    ADD COLUMN reviewed_at DATETIME NULL AFTER review_reason,
    ADD COLUMN review_version INT NOT NULL DEFAULT 1 AFTER reviewed_at;

CREATE TABLE note_ai_review_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    review_version INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    text_result VARCHAR(16) NULL,
    text_reason VARCHAR(200) NULL,
    image_pass TINYINT(1) NULL,
    internal_error VARCHAR(1000) NULL,
    lease_until DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_review_version (note_id, review_version),
    KEY idx_review_queue (status, lease_until, id),
    KEY idx_review_note (note_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
