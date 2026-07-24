ALTER TABLE sys_notification ADD COLUMN deduplication_key VARCHAR(150) NULL AFTER link;
ALTER TABLE sys_notification ADD UNIQUE KEY uk_notification_deduplication_key (deduplication_key);
