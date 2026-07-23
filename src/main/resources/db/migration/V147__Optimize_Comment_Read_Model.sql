-- Support batched liked-state enrichment for comment read models.
CREATE INDEX idx_comment_like_user_comment
    ON blog_comment_like (user_id, comment_id);
