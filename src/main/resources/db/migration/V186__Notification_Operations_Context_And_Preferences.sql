ALTER TABLE sys_notification
    ADD COLUMN object_type VARCHAR(24) NULL,
    ADD COLUMN object_id BIGINT NULL,
    ADD COLUMN actor_id BIGINT NULL,
    ADD COLUMN action VARCHAR(24) NULL,
    ADD KEY idx_notification_object (object_type, object_id);

ALTER TABLE sys_notification_delivery
    ADD COLUMN max_attempts INT NOT NULL DEFAULT 5,
    ADD COLUMN enqueue_generation INT NOT NULL DEFAULT 0;

CREATE TABLE sys_notification_category_preference (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(24) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL,
    email_enabled BOOLEAN NOT NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_notification_category_preference (user_id, category),
    CONSTRAINT fk_notification_category_preference_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_notification_delivery_retry (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    requested_by VARCHAR(255) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    previous_status VARCHAR(24) NOT NULL,
    previous_max_attempts INT NOT NULL,
    new_max_attempts INT NOT NULL,
    attempts_at_retry INT NOT NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_notification_delivery_retry_request (delivery_id, request_id),
    CONSTRAINT fk_notification_delivery_retry FOREIGN KEY (delivery_id) REFERENCES sys_notification_delivery(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
