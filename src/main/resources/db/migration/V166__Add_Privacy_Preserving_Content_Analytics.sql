-- Replace personally identifying network addresses with irreversible historical hashes.
ALTER TABLE `sys_visit_log`
    ADD COLUMN `visitor_hash` VARCHAR(64) NULL AFTER `id`;

UPDATE `sys_visit_log`
SET `visitor_hash` = SHA2(CONCAT('legacy-analytics:', `ip_address`), 256)
WHERE `visitor_hash` IS NULL;

ALTER TABLE `sys_visit_log`
    ADD COLUMN `session_id` VARCHAR(36) NULL AFTER `visitor_hash`;

UPDATE `sys_visit_log`
SET `session_id` = UUID()
WHERE `session_id` IS NULL;

ALTER TABLE `sys_visit_log`
    DROP COLUMN `ip_address`,
    MODIFY COLUMN `visitor_hash` VARCHAR(64) NOT NULL,
    MODIFY COLUMN `session_id` VARCHAR(36) NOT NULL,
    ADD INDEX `idx_visit_time_visitor` (`visit_time`, `visitor_hash`),
    ADD INDEX `idx_visit_session_time` (`session_id`, `visit_time`);

CREATE TABLE `blog_content_analytics_event` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` VARCHAR(36) NOT NULL,
    `visitor_hash` VARCHAR(64) NOT NULL,
    `post_id` BIGINT NOT NULL,
    `event_type` VARCHAR(32) NOT NULL,
    `progress_percent` INT NULL,
    `active_seconds` INT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `created_by` VARCHAR(255) NULL,
    `last_modified_by` VARCHAR(255) NULL,
    `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT `uk_content_analytics_session_post_event` UNIQUE (`session_id`, `post_id`, `event_type`),
    INDEX `idx_content_analytics_event_time` (`event_type`, `created_at`),
    INDEX `idx_content_analytics_post_event_time` (`post_id`, `event_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `blog_subscriber`
    ADD COLUMN `verified_at` DATETIME NULL;

UPDATE `blog_subscriber`
SET `verified_at` = `updated_at`
WHERE `status` = 'ACTIVE' AND `verified_at` IS NULL;
