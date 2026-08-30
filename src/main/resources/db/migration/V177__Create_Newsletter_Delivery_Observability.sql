CREATE TABLE blog_newsletter_delivery_batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(24) NOT NULL,
    recipient_count INT NOT NULL DEFAULT 0,
    queued_count INT NOT NULL DEFAULT 0,
    delivered_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    started_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_newsletter_batch_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_newsletter_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    subscriber_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    delivered_at DATETIME(3) NULL,
    created_by VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_modified_by VARCHAR(255) NULL,
    updated_at DATETIME(3) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_newsletter_delivery_batch_status (batch_id, status),
    CONSTRAINT fk_newsletter_delivery_batch FOREIGN KEY (batch_id) REFERENCES blog_newsletter_delivery_batch(id) ON DELETE CASCADE,
    CONSTRAINT fk_newsletter_delivery_subscriber FOREIGN KEY (subscriber_id) REFERENCES blog_subscriber(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
