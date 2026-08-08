CREATE TABLE sys_notification_preference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    comment_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    category_post_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    system_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_preference_user (user_id),
    CONSTRAINT fk_notification_preference_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
