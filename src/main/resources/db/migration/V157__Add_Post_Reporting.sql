CREATE TABLE blog_post_report (
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(80) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    handled_by VARCHAR(100) NULL,
    handled_at DATETIME(3) NULL,
    resolution_note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (post_id, reporter_id),
    KEY idx_post_report_status_created (status, created_at),
    KEY idx_post_report_reporter_created (reporter_id, created_at),
    CONSTRAINT fk_post_report_post FOREIGN KEY (post_id) REFERENCES blog_post(id),
    CONSTRAINT fk_post_report_user FOREIGN KEY (reporter_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
