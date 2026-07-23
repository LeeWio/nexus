-- Frontend-oriented comment presentation controls.
ALTER TABLE blog_comment
    ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_deleted_placeholder BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_comment_public_hot_sort
    ON blog_comment (is_deleted, post_id, parent_id, status, is_pinned, is_featured, likes_count, created_at, id);

CREATE INDEX idx_comment_guestbook_hot_sort
    ON blog_comment (is_deleted, parent_id, status, is_pinned, is_featured, likes_count, created_at, id);
