ALTER TABLE blog_webhook_log
    ADD COLUMN delivery_id VARCHAR(64) NULL AFTER webhook_id;

UPDATE blog_webhook_log
SET delivery_id = CONCAT('legacy-', id)
WHERE delivery_id IS NULL;

ALTER TABLE blog_webhook_log
    MODIFY COLUMN delivery_id VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uk_webhook_log_delivery_id UNIQUE (delivery_id);
