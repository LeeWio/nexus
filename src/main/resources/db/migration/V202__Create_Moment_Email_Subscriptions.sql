ALTER TABLE blog_moment ADD COLUMN published_once BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE blog_moment SET published_once = TRUE WHERE visibility = 'PUBLIC';

CREATE TABLE blog_moment_subscriber (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    status VARCHAR(20) NOT NULL,
    language VARCHAR(2) NOT NULL,
    generation BIGINT NOT NULL DEFAULT 0,
    verification_token VARCHAR(36),
    verification_requested_at DATETIME(6),
    verification_expires_at DATETIME(6),
    unsubscribe_token VARCHAR(36),
    verified_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_moment_subscriber_email (email),
    UNIQUE KEY uk_moment_subscriber_verify (verification_token),
    UNIQUE KEY uk_moment_subscriber_unsubscribe (unsubscribe_token),
    KEY idx_moment_subscriber_audience (status, verified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blog_moment_email_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subscriber_id BIGINT NOT NULL,
    moment_id BIGINT,
    generation BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    last_error VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_moment_email_recipient (subscriber_id, moment_id),
    KEY idx_moment_email_due (status, next_attempt_at, id),
    CONSTRAINT fk_moment_email_subscriber FOREIGN KEY (subscriber_id) REFERENCES blog_moment_subscriber(id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_email_moment FOREIGN KEY (moment_id) REFERENCES blog_moment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
