-- Support user comment center and status-filtered personal history.
CREATE INDEX idx_comment_user_status_created
    ON blog_comment (is_deleted, user_id, status, created_at, id);
