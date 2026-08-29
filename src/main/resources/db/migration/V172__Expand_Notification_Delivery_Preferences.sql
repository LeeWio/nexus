ALTER TABLE sys_notification_preference
    ADD COLUMN comment_email_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER comment_enabled,
    ADD COLUMN category_post_email_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER category_post_enabled,
    ADD COLUMN system_email_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER system_enabled;
