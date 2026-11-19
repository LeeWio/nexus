CREATE INDEX idx_notification_delivery_retention
    ON sys_notification_delivery (status, created_at, id);

CREATE INDEX idx_notification_delivery_retry_retention
    ON sys_notification_delivery_retry (created_at, id);
