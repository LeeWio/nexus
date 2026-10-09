ALTER TABLE sys_notification_delivery
    MODIFY COLUMN notification_id BIGINT NULL,
    ADD COLUMN deduplication_key VARCHAR(150) NULL,
    ADD COLUMN title VARCHAR(200) NULL,
    ADD COLUMN content TEXT NULL,
    ADD COLUMN link VARCHAR(500) NULL,
    ADD COLUMN next_attempt_at DATETIME(3) NULL,
    ADD UNIQUE KEY uk_notification_delivery_event (deduplication_key);

UPDATE sys_notification_delivery delivery
JOIN sys_notification notification ON notification.id = delivery.notification_id
SET delivery.title = notification.title,
    delivery.content = notification.content,
    delivery.link = notification.link,
    delivery.next_attempt_at = COALESCE(delivery.updated_at, delivery.created_at);

ALTER TABLE sys_notification_delivery
    MODIFY COLUMN title VARCHAR(200) NOT NULL,
    MODIFY COLUMN content TEXT NOT NULL,
    MODIFY COLUMN next_attempt_at DATETIME(3) NOT NULL,
    ADD KEY idx_notification_delivery_due (status, next_attempt_at);
