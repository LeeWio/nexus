CREATE TABLE blog_reading_list_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reading_list_user_post (user_id, post_id),
    KEY idx_reading_list_user_recent (user_id, created_at DESC),
    CONSTRAINT fk_reading_list_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_list_post FOREIGN KEY (post_id) REFERENCES blog_post(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
