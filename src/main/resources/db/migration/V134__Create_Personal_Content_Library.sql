CREATE TABLE IF NOT EXISTS blog_reading_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    progress_percent TINYINT UNSIGNED NOT NULL DEFAULT 0,
    position_anchor VARCHAR(500) NULL,
    last_read_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reading_history_user_post (user_id, post_id),
    KEY idx_reading_history_user_recent (user_id, last_read_at DESC),
    CONSTRAINT fk_reading_history_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_history_post FOREIGN KEY (post_id) REFERENCES blog_post(id) ON DELETE CASCADE,
    CONSTRAINT chk_reading_history_progress CHECK (progress_percent BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS blog_post_collection (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(300) NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_collection_user_name (user_id, name),
    KEY idx_post_collection_user_recent (user_id, created_at DESC),
    CONSTRAINT fk_post_collection_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS blog_post_collection_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    collection_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_collection_item_collection_post (collection_id, post_id),
    KEY idx_collection_item_recent (collection_id, created_at DESC),
    CONSTRAINT fk_collection_item_collection FOREIGN KEY (collection_id) REFERENCES blog_post_collection(id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_item_post FOREIGN KEY (post_id) REFERENCES blog_post(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
