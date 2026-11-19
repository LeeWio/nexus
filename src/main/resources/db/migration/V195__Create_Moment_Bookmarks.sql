CREATE TABLE blog_moment_bookmark (
    user_id BIGINT NOT NULL,
    moment_id BIGINT NOT NULL,
    -- NULL records a removal so a legacy browser import cannot restore it.
    saved_at DATETIME(6) NULL,
    PRIMARY KEY (user_id, moment_id),
    KEY idx_moment_bookmark_user_saved (user_id, saved_at DESC, moment_id DESC),
    CONSTRAINT fk_moment_bookmark_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_bookmark_moment FOREIGN KEY (moment_id) REFERENCES blog_moment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
