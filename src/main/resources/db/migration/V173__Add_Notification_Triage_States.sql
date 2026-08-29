ALTER TABLE sys_notification
    ADD COLUMN is_saved BOOLEAN NOT NULL DEFAULT FALSE AFTER is_read,
    ADD COLUMN completed_at DATETIME(3) NULL AFTER read_at,
    ADD INDEX idx_notification_inbox (user_id, completed_at, is_saved, created_at);
