CREATE TABLE blog_webhook_log (
    id BIGINT NOT NULL,
    webhook_id BIGINT NOT NULL,
    event VARCHAR(255) NOT NULL,
    url TEXT,
    request_payload TEXT,
    response_payload TEXT,
    response_code INT,
    is_success BIT(1),
    error_message VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BIT(1) DEFAULT 0 NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_webhook_log_webhook FOREIGN KEY (webhook_id) REFERENCES blog_webhook (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_webhook_log_webhook_id ON blog_webhook_log(webhook_id);
CREATE INDEX idx_webhook_log_created_at ON blog_webhook_log(created_at);
