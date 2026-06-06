CREATE TABLE blog_link_check_log (
    id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT,
    source_title VARCHAR(200),
    status_code INT,
    is_broken BIT(1) NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BIT(1) DEFAULT 0 NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_link_check_url ON blog_link_check_log(url);
CREATE INDEX idx_link_check_source ON blog_link_check_log(source_type, source_id);
CREATE INDEX idx_link_check_broken ON blog_link_check_log(is_broken);
