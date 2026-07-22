ALTER TABLE blog_post_revision
    ADD COLUMN change_type VARCHAR(50) NULL AFTER version_number,
    ADD COLUMN change_summary VARCHAR(500) NULL AFTER change_type,
    ADD COLUMN content_hash CHAR(64) NULL AFTER change_summary;

CREATE INDEX idx_revision_post_hash ON blog_post_revision (post_id, content_hash);
