-- Store stable idempotency semantics for comment submissions.
CREATE TABLE blog_comment_idempotency (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_by VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50),
    updated_at DATETIME NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_code INT NULL,
    response_message VARCHAR(255) NULL,
    comment_id BIGINT NULL,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_idempotency_user_key (user_id, idempotency_key),
    KEY idx_comment_idempotency_created (created_at),
    KEY idx_comment_idempotency_comment (comment_id),
    CONSTRAINT fk_comment_idempotency_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_comment_idempotency_comment FOREIGN KEY (comment_id) REFERENCES blog_comment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
