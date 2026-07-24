ALTER TABLE blog_post ADD COLUMN archive_reason VARCHAR(1000) NULL AFTER reviewed_by;
ALTER TABLE blog_post ADD COLUMN archived_at DATETIME(6) NULL AFTER archive_reason;
ALTER TABLE blog_post ADD COLUMN archived_by BIGINT NULL AFTER archived_at;
ALTER TABLE blog_post ADD CONSTRAINT fk_post_archived_by FOREIGN KEY (archived_by) REFERENCES sys_user(id) ON DELETE SET NULL;

CREATE INDEX idx_post_archived_by ON blog_post (archived_by);
CREATE INDEX idx_post_archived_at ON blog_post (status, archived_at DESC);
