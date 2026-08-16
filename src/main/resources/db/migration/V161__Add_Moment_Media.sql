CREATE TABLE blog_moment_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    alt_text VARCHAR(300) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    last_modified_by VARCHAR(255) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_moment_media_position UNIQUE (moment_id, sort_order),
    CONSTRAINT uk_moment_media_file UNIQUE (moment_id, file_id),
    CONSTRAINT fk_moment_media_moment FOREIGN KEY (moment_id) REFERENCES blog_moment(id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_media_file FOREIGN KEY (file_id) REFERENCES sys_file(id) ON DELETE RESTRICT,
    INDEX idx_moment_media_moment_order (moment_id, sort_order),
    INDEX idx_moment_media_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
