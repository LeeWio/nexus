CREATE TABLE blog_post_editor_draft (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    identifier VARCHAR(19) NOT NULL,
    revision VARCHAR(36) NOT NULL,
    content JSON NOT NULL,
    post_data JSON NOT NULL,
    saved_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_editor_draft_owner UNIQUE (user_id, identifier),
    CONSTRAINT fk_post_editor_draft_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
