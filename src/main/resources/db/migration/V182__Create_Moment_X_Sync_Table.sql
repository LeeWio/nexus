CREATE TABLE blog_moment_x_sync (
    id BIGINT NOT NULL AUTO_INCREMENT,
    moment_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    x_post_id VARCHAR(64) NULL,
    x_post_url VARCHAR(255) NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    posted_at DATETIME(3) NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_moment_x_sync_moment (moment_id),
    KEY idx_moment_x_sync_status (status, updated_at),
    CONSTRAINT fk_moment_x_sync_moment FOREIGN KEY (moment_id) REFERENCES blog_moment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
