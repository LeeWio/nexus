ALTER TABLE blog_subscriber
    ADD COLUMN verification_expires_at DATETIME NULL AFTER verification_token;

UPDATE blog_subscriber
SET verification_expires_at = CURRENT_TIMESTAMP + INTERVAL '24' HOUR
WHERE status = 'PENDING' AND verification_token IS NOT NULL;

CREATE INDEX idx_subscriber_status_id ON blog_subscriber(status, id);
CREATE INDEX idx_subscriber_verification_token ON blog_subscriber(verification_token);
CREATE INDEX idx_subscriber_unsubscribe_token ON blog_subscriber(unsubscribe_token);
