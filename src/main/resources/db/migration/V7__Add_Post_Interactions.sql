ALTER TABLE blog_post ADD COLUMN likes_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE blog_post ADD COLUMN favorites_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE user_favorite_post (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_post FOREIGN KEY (post_id) REFERENCES blog_post(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
