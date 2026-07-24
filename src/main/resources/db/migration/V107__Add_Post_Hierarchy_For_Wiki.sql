ALTER TABLE blog_post ADD COLUMN parent_id BIGINT;
ALTER TABLE blog_post ADD COLUMN path VARCHAR(1000);
ALTER TABLE blog_post
    ADD CONSTRAINT fk_blog_post_parent
    FOREIGN KEY (parent_id) REFERENCES blog_post(id);

CREATE INDEX idx_blog_post_path ON blog_post(path(255));
