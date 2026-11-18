-- 管理端分析面板查询索引（执行一次）

ALTER TABLE fans
    ADD INDEX idx_fans_create_time_user_id (create_time, user_id);

ALTER TABLE note
    ADD INDEX idx_note_create_time_status_creator (create_time, status, creator_id);

ALTER TABLE note_count
    ADD INDEX idx_note_count_like_total_note_id (like_total, note_id);
