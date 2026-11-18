-- Apply once before deploying the AI conversation persistence release (MySQL 8).
ALTER TABLE ai_conversation ADD COLUMN agent_type VARCHAR(255) NULL COMMENT '最后使用的助手类型';
ALTER TABLE ai_message
    ADD COLUMN user_id BIGINT NULL COMMENT '用户ID，历史归属不明时为空',
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' COMMENT 'GENERATING/COMPLETED/STOPPED/DISCONNECTED/FAILED',
    ADD COLUMN error_message TEXT NULL,
    ADD COLUMN metadata LONGTEXT NULL COMMENT '图片、发布选项和PPT信息JSON',
    ADD INDEX idx_user_conversation (user_id, conversation_id, deleted, id);
ALTER TABLE ai_ppt_inst
    ADD COLUMN user_id BIGINT NULL COMMENT '用户ID，历史归属不明时为空',
    ADD INDEX idx_user_conversation (user_id, conversation_id);

-- Never guess ownership when multiple users share an old conversation ID.
UPDATE ai_message m JOIN (
    SELECT conversation_id, MIN(user_id) AS user_id
    FROM ai_conversation GROUP BY conversation_id HAVING COUNT(DISTINCT user_id) = 1
) owners ON owners.conversation_id = m.conversation_id SET m.user_id = owners.user_id;
UPDATE ai_ppt_inst p JOIN (
    SELECT conversation_id, MIN(user_id) AS user_id
    FROM ai_conversation GROUP BY conversation_id HAVING COUNT(DISTINCT user_id) = 1
) owners ON owners.conversation_id = p.conversation_id SET p.user_id = owners.user_id;
UPDATE ai_conversation c JOIN (
    SELECT user_id, conversation_id, MAX(id) AS latest_id
    FROM ai_message WHERE deleted = 0 AND user_id IS NOT NULL GROUP BY user_id, conversation_id
) latest ON latest.user_id = c.user_id AND latest.conversation_id = c.conversation_id
JOIN ai_message m ON m.id = latest.latest_id
SET c.agent_type = m.agent_type;

-- Review these rows separately; application queries exclude unowned records.
SELECT id, message_id, conversation_id FROM ai_message WHERE user_id IS NULL;
SELECT id, conversation_id FROM ai_ppt_inst WHERE user_id IS NULL;
