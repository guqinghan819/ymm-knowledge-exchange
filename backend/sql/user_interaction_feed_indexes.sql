-- 个人主页点赞、收藏列表按用户和互动时间查询使用。
ALTER TABLE note_like
    ADD INDEX idx_note_like_user_status_time_note (user_id, status, create_time, note_id);

ALTER TABLE note_collection
    ADD INDEX idx_note_collection_user_status_time_note (user_id, status, create_time, note_id);
