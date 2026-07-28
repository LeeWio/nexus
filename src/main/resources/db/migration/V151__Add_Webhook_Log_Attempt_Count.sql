ALTER TABLE blog_webhook_log ADD COLUMN attempt_count INT NOT NULL DEFAULT 1 COMMENT 'Number of delivery attempts';
