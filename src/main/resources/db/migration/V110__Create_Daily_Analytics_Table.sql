CREATE TABLE blog_daily_analytics (
    id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0,
    post_views BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BIT(1) DEFAULT 0 NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
