-- Support hierarchy integrity checks and materialized-path maintenance.
CREATE INDEX idx_post_parent_deleted ON blog_post (parent_id, is_deleted);
CREATE INDEX idx_comment_parent_deleted ON blog_comment (parent_id, is_deleted);

-- Support the dominant public and moderation access paths.
CREATE INDEX idx_post_public_page ON blog_post (is_deleted, status, created_at, id);
CREATE INDEX idx_post_category_page ON blog_post (is_deleted, status, category_id, created_at, id);
CREATE INDEX idx_comment_post_status ON blog_comment (is_deleted, post_id, status, created_at, id);
CREATE INDEX idx_comment_moderation ON blog_comment (is_deleted, status, created_at, id);
