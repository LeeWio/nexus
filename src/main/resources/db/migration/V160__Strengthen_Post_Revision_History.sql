ALTER TABLE blog_post
    ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE blog_post_revision
    ADD COLUMN parent_revision_id BIGINT NULL AFTER post_id,
    ADD COLUMN base_version_number INT NULL AFTER version_number,
    ADD COLUMN source_revision_id BIGINT NULL AFTER base_version_number,
    ADD COLUMN revision_kind VARCHAR(32) NOT NULL DEFAULT 'LEGACY' AFTER change_type,
    ADD COLUMN snapshot_json LONGTEXT NULL AFTER content_hash,
    ADD COLUMN snapshot_hash CHAR(64) NULL AFTER snapshot_json;

CREATE INDEX idx_revision_post_created ON blog_post_revision (post_id, created_at DESC);
CREATE INDEX idx_revision_parent ON blog_post_revision (parent_revision_id);

ALTER TABLE blog_post_revision
    ADD CONSTRAINT fk_revision_parent
        FOREIGN KEY (parent_revision_id) REFERENCES blog_post_revision(id) ON DELETE SET NULL;
