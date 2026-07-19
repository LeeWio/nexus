ALTER TABLE blog_post
    ADD COLUMN scheduled_at DATETIME(3) NULL AFTER published_at,
    MODIFY COLUMN status ENUM(
        'ARCHIVED',
        'DRAFT',
        'PENDING_REVIEW',
        'PUBLISHED',
        'REJECTED',
        'SCHEDULED'
    ) NOT NULL;

CREATE INDEX idx_blog_post_scheduled_due
    ON blog_post (status, scheduled_at, id);
