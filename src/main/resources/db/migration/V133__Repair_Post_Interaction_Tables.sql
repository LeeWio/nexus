-- Repair legacy databases that were managed by Hibernate before Flyway adoption.
-- These relation tables are accessed through JdbcTemplate and therefore cannot be
-- inferred or created by Hibernate entity schema updates.
CREATE TABLE IF NOT EXISTS blog_post_like (
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    KEY idx_post_like_user_created (user_id, created_at),
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES blog_post(id),
    CONSTRAINT fk_post_like_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS blog_post_favorite (
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    KEY idx_post_favorite_user_created (user_id, created_at),
    CONSTRAINT fk_post_favorite_post FOREIGN KEY (post_id) REFERENCES blog_post(id),
    CONSTRAINT fk_post_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
