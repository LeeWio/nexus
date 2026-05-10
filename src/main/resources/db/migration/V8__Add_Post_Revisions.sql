CREATE TABLE blog_post_revision (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content LONGTEXT NOT NULL,
    version_number INT NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_revision_post FOREIGN KEY (post_id) REFERENCES blog_post(id) ON DELETE CASCADE,
    CONSTRAINT fk_revision_user FOREIGN KEY (created_by) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional index for faster lookups by post
CREATE INDEX idx_revision_post ON blog_post_revision(post_id);
