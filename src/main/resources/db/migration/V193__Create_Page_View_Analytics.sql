-- First-party page views. One row is one accepted navigation, not an API hit.
CREATE TABLE `blog_page_view` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `viewed_at` DATETIME(6) NOT NULL,
    `path` VARCHAR(255) NOT NULL,
    `title` VARCHAR(160) NULL,
    `session_id` VARCHAR(36) NOT NULL,
    `visitor_hash` VARCHAR(64) NOT NULL,
    `ip_address` VARCHAR(45) NULL,
    `country` VARCHAR(80) NULL,
    `region` VARCHAR(80) NULL,
    `city` VARCHAR(80) NULL,
    `browser` VARCHAR(40) NULL,
    `os_name` VARCHAR(40) NULL,
    `device_type` VARCHAR(16) NULL,
    `client_name` VARCHAR(40) NULL,
    `referrer` VARCHAR(500) NULL,
    `referrer_host` VARCHAR(255) NULL,
    `source_channel` VARCHAR(32) NOT NULL,
    `user_id` BIGINT NULL,
    `traffic_class` VARCHAR(16) NOT NULL,
    `user_agent` VARCHAR(500) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `created_by` VARCHAR(255) NULL,
    `last_modified_by` VARCHAR(255) NULL,
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_page_view_time_class` (`viewed_at`, `traffic_class`),
    INDEX `idx_page_view_session_time` (`session_id`, `viewed_at`),
    INDEX `idx_page_view_visitor_time` (`visitor_hash`, `viewed_at`),
    INDEX `idx_page_view_path_time` (`path`, `viewed_at`),
    INDEX `idx_page_view_channel_time` (`source_channel`, `viewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `blog_daily_analytics`
    ADD COLUMN `sessions` BIGINT NOT NULL DEFAULT 0 AFTER `uv`,
    ADD COLUMN `owner_pv` BIGINT NOT NULL DEFAULT 0 AFTER `comment_count`,
    ADD COLUMN `bot_pv` BIGINT NOT NULL DEFAULT 0 AFTER `owner_pv`;
