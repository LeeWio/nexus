ALTER TABLE sys_notification
    ADD COLUMN is_visible BOOLEAN NOT NULL DEFAULT TRUE AFTER is_read,
    ADD INDEX idx_notification_visible_inbox (user_id, is_visible, completed_at, created_at);
