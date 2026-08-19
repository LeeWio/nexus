CREATE TABLE moment_topic (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(80) NOT NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_moment_topic_slug UNIQUE (slug),
    INDEX idx_moment_topic_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE moment_topic_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_moment_topic_relation UNIQUE (moment_id, topic_id),
    CONSTRAINT uk_moment_topic_position UNIQUE (moment_id, sort_order),
    CONSTRAINT fk_moment_topic_relation_moment FOREIGN KEY (moment_id) REFERENCES blog_moment(id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_topic_relation_topic FOREIGN KEY (topic_id) REFERENCES moment_topic(id) ON DELETE RESTRICT,
    INDEX idx_moment_topic_relation_topic_moment (topic_id, moment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
