ALTER TABLE sys_notification
    ADD COLUMN read_at DATETIME(6) NULL AFTER is_read;

CREATE INDEX idx_notification_user_read_created
    ON sys_notification(user_id, is_read, created_at DESC);

-- Drop old indexes if they are redundant (commented out for cross-database / H2 compatibility)
-- DROP INDEX idx_notification_user_id ON sys_notification;
-- DROP INDEX idx_notification_is_read ON sys_notification;
