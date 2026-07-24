ALTER TABLE blog_post ADD COLUMN review_comment VARCHAR(1000) NULL AFTER published_at;
ALTER TABLE blog_post ADD COLUMN reviewed_at DATETIME NULL AFTER review_comment;
ALTER TABLE blog_post ADD COLUMN reviewed_by BIGINT NULL AFTER reviewed_at;
ALTER TABLE blog_post ADD CONSTRAINT fk_post_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES sys_user(id);

CREATE INDEX idx_post_reviewed_by ON blog_post (reviewed_by);
