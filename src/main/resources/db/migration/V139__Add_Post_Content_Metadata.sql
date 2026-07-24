ALTER TABLE blog_post ADD COLUMN auto_summary VARCHAR(500) NULL AFTER summary;
ALTER TABLE blog_post ADD COLUMN word_count INT NOT NULL DEFAULT 0 AFTER favorites_count;
ALTER TABLE blog_post ADD COLUMN reading_time_minutes INT NOT NULL DEFAULT 1 AFTER word_count;
ALTER TABLE blog_post ADD COLUMN content_hash CHAR(64) NULL AFTER reading_time_minutes;
ALTER TABLE blog_post ADD COLUMN toc TEXT NULL AFTER content_hash;

CREATE INDEX idx_post_reading_time ON blog_post (reading_time_minutes, id);
CREATE INDEX idx_post_content_hash ON blog_post (content_hash);
