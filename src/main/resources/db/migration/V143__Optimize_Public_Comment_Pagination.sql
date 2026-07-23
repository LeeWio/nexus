-- Optimize public comment pagination and lazy reply expansion.
CREATE INDEX idx_comment_public_roots
    ON blog_comment (is_deleted, post_id, parent_id, status, created_at, id);

CREATE INDEX idx_comment_public_replies
    ON blog_comment (is_deleted, parent_id, status, created_at, id);
