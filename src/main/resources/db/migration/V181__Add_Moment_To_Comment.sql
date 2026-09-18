-- Allow comments to target moments independently from posts/guestbook.
ALTER TABLE blog_comment
	ADD COLUMN moment_id BIGINT NULL AFTER post_id;

ALTER TABLE blog_comment
	ADD CONSTRAINT fk_comment_moment FOREIGN KEY (moment_id) REFERENCES blog_moment (id);

CREATE INDEX idx_comment_moment_root_status_id
	ON blog_comment (moment_id, parent_id, status, id);

CREATE INDEX idx_comment_moment_status_created
	ON blog_comment (moment_id, status, created_at);
