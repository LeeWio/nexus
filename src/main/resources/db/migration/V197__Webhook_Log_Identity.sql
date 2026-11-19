-- Match the WebhookLog identity mapping on databases managed solely by Flyway.
ALTER TABLE blog_webhook_log MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
