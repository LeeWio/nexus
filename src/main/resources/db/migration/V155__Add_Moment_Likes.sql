CREATE TABLE IF NOT EXISTS blog_moment_like (
    moment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (moment_id, user_id),
    KEY idx_moment_like_user_created (user_id, created_at),
    CONSTRAINT fk_moment_like_moment FOREIGN KEY (moment_id) REFERENCES blog_moment(id),
    CONSTRAINT fk_moment_like_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
