-- Add moderation audit trail and report lifecycle state.
ALTER TABLE blog_comment_report ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
ALTER TABLE blog_comment_report ADD COLUMN handled_by VARCHAR(100) NULL;
ALTER TABLE blog_comment_report ADD COLUMN handled_at DATETIME NULL;
ALTER TABLE blog_comment_report ADD COLUMN resolution_note VARCHAR(500) NULL;

CREATE INDEX idx_comment_report_status_created
    ON blog_comment_report (status, created_at);

CREATE TABLE blog_comment_moderation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_by VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50),
    updated_at DATETIME NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    comment_id BIGINT NOT NULL,
    moderator_username VARCHAR(100),
    action VARCHAR(40) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    reason VARCHAR(120),
    batch_id VARCHAR(36),
    note TEXT,
    PRIMARY KEY (id),
    KEY idx_comment_moderation_log_comment_created (comment_id, created_at),
    KEY idx_comment_moderation_log_action_created (action, created_at),
    KEY idx_comment_moderation_log_batch (batch_id),
    CONSTRAINT fk_comment_moderation_log_comment FOREIGN KEY (comment_id) REFERENCES blog_comment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
