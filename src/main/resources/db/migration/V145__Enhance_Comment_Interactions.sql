-- Add interaction and edit metadata to comments.
ALTER TABLE blog_comment ADD COLUMN likes_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE blog_comment ADD COLUMN reports_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE blog_comment ADD COLUMN edited_at DATETIME NULL;
ALTER TABLE blog_comment ADD COLUMN edit_count INT NOT NULL DEFAULT 0;

CREATE TABLE blog_comment_like (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id),
    KEY idx_comment_like_user_created (user_id, created_at),
    CONSTRAINT fk_comment_like_comment FOREIGN KEY (comment_id) REFERENCES blog_comment(id),
    CONSTRAINT fk_comment_like_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_comment_report (
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, reporter_id),
    KEY idx_comment_report_reporter_created (reporter_id, created_at),
    KEY idx_comment_report_comment_created (comment_id, created_at),
    CONSTRAINT fk_comment_report_comment FOREIGN KEY (comment_id) REFERENCES blog_comment(id),
    CONSTRAINT fk_comment_report_user FOREIGN KEY (reporter_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comment_public_like_sort
    ON blog_comment (is_deleted, post_id, parent_id, status, likes_count, created_at, id);
